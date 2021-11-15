package hu.nagygm.oauth2.server.handler

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.MACSigner
import hu.nagygm.oauth2.client.OAuth2Authorization
import hu.nagygm.oauth2.client.OAuth2AuthorizationRepository
import hu.nagygm.oauth2.client.registration.*
import hu.nagygm.oauth2.util.completeAndJoinChildren
import kotlinx.coroutines.*
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator
import org.springframework.security.oauth2.core.*
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames
import org.springframework.stereotype.Service
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.server.*
import java.security.SecureRandom
import java.time.Instant
import java.util.*


@Service
class TokenHandler(
    @Autowired val clientRegistrationRepository: ClientRegistrationRepository,
    @Autowired val grantRequestService: GrantRequestService,
    @Autowired val oAuth2AuthorizationRepository: OAuth2AuthorizationRepository,
    @Autowired @Qualifier("tokenJsonMapper") val mapper: ObjectMapper
) {

    private val codeGenerator = Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 32)

    suspend fun acquireToken(request: ServerRequest): ServerResponse {
        val response = validate(formToTokenRequest(request.awaitFormData()))
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
            .headers { it["Cache-Control"] = "no-store"; it["Pragma"] = "no-cache" }
            .bodyValue(response).awaitFirst()
    }

    suspend fun revokeToken(request: ServerRequest): ServerResponse = TODO("Implement revocation")

    private suspend fun validate(request: TokenRequest): TokenResponse {
        if (request.grantType == AuthorizationGrantType.AUTHORIZATION_CODE.value) {
            return validateAuthorizationGrantType(request)
        } else {
            throw OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.UNSUPPORTED_GRANT_TYPE))
        }
    }

    private suspend fun validateAuthorizationGrantType(request: TokenRequest): TokenResponse {
        if (!request.code.isNullOrBlank() && !request.clientId.isNullOrBlank()) {
            val fetchClientdataJobs = Job()
            fetchClientdataJobs.plus(Dispatchers.IO)
            var clientRegistration = withContext(fetchClientdataJobs) {
                return@withContext async { clientRegistrationRepository.findByClientId(request.clientId) }
            }.await()
            var grantRequest = withContext(fetchClientdataJobs) {
                return@withContext async {
                    grantRequestService.getGrantRequestByCodeAndClientId(
                        request.code,
                        request.clientId
                    )
                }
            }.await()

            fetchClientdataJobs.completeAndJoinChildren()

            if (clientRegistration == null) {
                throw (OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT)))
            }
            if (!clientRegistration.authorizationGrantTypes.contains(AuthorizationGrantType.AUTHORIZATION_CODE) &&
                grantRequest.redirectUri != request.redirectUri
            ) {
                throw (OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT)))
            }
            if (Instant.now().isBefore(grantRequest.codeCreatedAt?.plusSeconds(600L))) {
                return generateToken(clientRegistration, grantRequest)
            } else {
                throw (OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT)))
            }
        } else {
            throw (OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT)))
        }
    }

    private suspend fun generateToken(
        clientRegistration: ClientRegistration,
        grantRequest: GrantRequest,

    ): TokenResponse {
        val now = Instant.now()
        val accessToken = OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            codeGenerator.generateKey(),
            now,
            now.plusSeconds(
                (clientRegistration.clientConfiguration[ClientConfigurationParams.ACCESS_TOKEN_LIFETIME] as Int).toLong()
            ),
            grantRequest.scopes //TODO fix get scopes from consent
        )
        val token = OAuth2Authorization(
            UUID.randomUUID().toString(),
            clientRegistration,
            clientRegistration.clientId,
            AuthorizationGrantType.AUTHORIZATION_CODE,
            hashSetOf(accessToken), emptyMap()
        )
        oAuth2AuthorizationRepository.save(
            token
        )
        return TokenResponse(jwtEncoder(accessToken),
            OAuth2AccessToken.TokenType.BEARER.value,
            3600,
            grantRequest.scopes.reduce { s1, s2 -> "$s1 $s2" },
        )
    }

    private fun jwtEncoder(token: OAuth2AccessToken): String {
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        val jwsObject = JWSObject(
            JWSHeader(JWSAlgorithm.HS256),
            Payload(mapper.writeValueAsString(token))
        )

        val sharedKey = ByteArray(32)
        SecureRandom().nextBytes(sharedKey)

        jwsObject.sign(MACSigner(sharedKey))
        return jwsObject.serialize(false)
    }

    private fun formToTokenRequest(map: MultiValueMap<String, String>): TokenRequest {
        return TokenRequest(
            map.getFirst(OAuth2ParameterNames.GRANT_TYPE),
            map.getFirst(OAuth2ParameterNames.CODE),
            map.getFirst(OAuth2ParameterNames.REDIRECT_URI),
            map.getFirst(OAuth2ParameterNames.CLIENT_ID),
            map.getFirst(PkceParameterNames.CODE_VERIFIER)
        )
    }

    data class TokenRequest(
        @field:JsonProperty("grant_type")
        val grantType: String?,
        @field:JsonProperty("code")
        val code: String?,
        @field:JsonProperty("redirect_uri")
        val redirectUri: String?,
        @field:JsonProperty("client_id")
        val clientId: String?,
        @field:JsonProperty("code_verifier")
        val codeVerifier: String?
    )

    data class TokenResponse(
        @field:JsonProperty("access_token")
        val accessToken: String,
        @field:JsonProperty("token_type")
        val tokenType: String,
        @field:JsonProperty("expires_in")
        val expiresIn: Int,
        val scope: String
    )
}
