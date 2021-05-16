package hu.nagygm.oauth2.server.web

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator
import org.springframework.security.oauth2.core.*
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.stereotype.Service
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.server.*
import java.security.SecureRandom
import java.time.Instant
import java.util.*


@Service
open class TokenHandler(
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
            if (!request.code.isNullOrBlank() && !request.clientId.isNullOrBlank()) {
                val clientRegistration = clientRegistrationRepository.findByClientId(request.clientId)
                val grantRequest = grantRequestService.getGrantRequestByCodeAndClientId(request.code, request.clientId)
                if (clientRegistration == null) {
                    throw (OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT)))
                }
                if (!clientRegistration.authorizationGrantTypes.contains(AuthorizationGrantType.AUTHORIZATION_CODE) &&
                    grantRequest.redirectUri != request.redirectUri
                ) {
                    throw (OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT)))
                }
                if (Instant.now().isBefore(grantRequest.codeCreatedAt?.plusSeconds(600L))) {
                    return generateToken(request, clientRegistration, grantRequest)
                } else {
                    throw (OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT)))
                }
            } else {
                throw (OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT)))
            }
        } else {
            throw OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.UNSUPPORTED_GRANT_TYPE))
        }
    }

    private suspend fun generateToken(
        tokenRequest: TokenRequest,
        clientRegistration: ClientRegistration,
        grantRequest: GrantRequest
    ): TokenResponse {
        val now = Instant.now()
        val accessToken = OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER, codeGenerator.generateKey(), now, now.plusSeconds(
                (clientRegistration.clientConfiguration[ClientConfigurationParams.ACCESS_TOKEN_LIFETIME] as Int).toLong()
            ), grantRequest.scopes //TODO fix get scopes from consent
        )
        val token = OAuth2Authorization(
            UUID.randomUUID().toString(), clientRegistration, clientRegistration.clientId, AuthorizationGrantType.AUTHORIZATION_CODE,
            hashSetOf(accessToken), emptyMap()
        )
        oAuth2AuthorizationRepository.save(
            token
        )
        return TokenResponse(jwtEncoder(accessToken), "authorization_code", 3600, "test")

    }

    private suspend fun jwtEncoder(token: OAuth2AccessToken): String {
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

    private suspend fun formToTokenRequest(map: MultiValueMap<String, String>): TokenRequest {
        return TokenRequest(
            map.getFirst(OAuth2ParameterNames.GRANT_TYPE),
            map.getFirst(OAuth2ParameterNames.CODE),
            map.getFirst(OAuth2ParameterNames.REDIRECT_URI),
            map.getFirst(OAuth2ParameterNames.CLIENT_ID),
        )
    }

    data class TokenRequest(
        val grantType: String?,
        val code: String?,
        val redirectUri: String?,
        val clientId: String?
    )

    data class TokenResponse(
        val accessToken: String,
        val tokenType: String,
        val expiresIn: Int,
        val scope: String
    )
}
