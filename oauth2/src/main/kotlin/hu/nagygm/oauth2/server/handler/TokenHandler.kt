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
import hu.nagygm.oauth2.server.GrantRequest
import hu.nagygm.oauth2.server.GrantRequestService
import hu.nagygm.oauth2.server.security.pkce.CodeChallengeMethods
import hu.nagygm.oauth2.server.security.pkce.CodeVerifier
import hu.nagygm.oauth2.util.completeAndJoinChildren
import kotlinx.coroutines.*
import kotlinx.coroutines.reactive.awaitFirst
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.*
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.*
import java.io.UnsupportedEncodingException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.time.Instant
import java.util.*
import java.util.regex.Matcher


@Service
class TokenHandler(
    @Autowired val clientRegistrationRepository: ClientRegistrationRepository,
    @Autowired val grantRequestService: GrantRequestService,
    @Autowired val oAuth2AuthorizationRepository: OAuth2AuthorizationRepository,
    @Autowired @Qualifier("tokenJsonMapper") val mapper: ObjectMapper,
    @Autowired val passwordEncoder: PasswordEncoder
) {

    companion object {
        private val logger = LogFactory.getLog(TokenHandler::class.java)
    }
    private val codeGenerator = Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 32)

    suspend fun acquireToken(request: ServerRequest): ServerResponse {
        val response = validate(formToTokenRequest(request))
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
            }.await() ?: throw OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, "Invalid code", ""))

            fetchClientdataJobs.completeAndJoinChildren()

            if (clientRegistration == null) {
                throw (OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT)))
            }
            if (clientRegistration.secret.isNotEmpty() && !passwordEncoder.matches(request.clientSecret, clientRegistration.secret)) {
                throw OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.ACCESS_DENIED, "Invalid Authorization header", ""))
            }

            if (!clientRegistration.authorizationGrantTypes.contains(AuthorizationGrantType.AUTHORIZATION_CODE) &&
                grantRequest.redirectUri != request.redirectUri
            ) {
                throw (OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT)))
            }

            if (!validateCodeChallenge(grantRequest, request)) {throw OAuth2AuthorizationException(OAuth2Error(
                OAuth2ErrorCodes.INVALID_REQUEST,
                "Invalid code_challenge",
                "https://datatracker.ietf.org/doc/html/rfc7636#section-4.4.1"))
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

    private fun validateCodeChallenge(grantRequest: GrantRequest, tokenRequest: TokenRequest): Boolean {
        val codeVerifier = tokenRequest.codeVerifier
        if (grantRequest.codeChallenge != null && grantRequest.codeChallengeMethod != null) {
            val matcher: Matcher = CodeVerifier.CODE_CHALLENGE_PATTERN.matcher(grantRequest.codeChallenge);
            if (tokenRequest.codeVerifier == null || tokenRequest.codeVerifier.isEmpty() || !matcher.matches()) {
                return false
            }
            return when (grantRequest.codeChallengeMethod) {
                CodeChallengeMethods.S256.value -> {
                    val digestedCodeChallenge: String = CodeVerifier.processCodeChallenge(tokenRequest.codeVerifier)
                    grantRequest.codeChallenge == digestedCodeChallenge
                }
                CodeChallengeMethods.PLAIN.value -> {
                    grantRequest.codeChallenge == codeVerifier
                }
                else -> {
                    false
                }
            }
        } else {
            //TODO add checking for compulsory PKCE (for public client)
            return true
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
            grantRequest.acceptedScopes
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
        return TokenResponse(
            jwtEncoder(accessToken),
            OAuth2AccessToken.TokenType.BEARER.value,
            3600,
            grantRequest.acceptedScopes.reduce { s1, s2 -> "$s1 $s2" },
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

    private suspend fun formToTokenRequest(request: ServerRequest): TokenRequest {
        val map = request.awaitFormData()
        val credentials = extractClientAuthenticationData(request)
        return TokenRequest(
            map.getFirst(OAuth2ParameterNames.GRANT_TYPE),
            map.getFirst(OAuth2ParameterNames.CODE),
            map.getFirst(OAuth2ParameterNames.REDIRECT_URI),
            credentials.clientId ?: map.getFirst(OAuth2ParameterNames.CLIENT_ID),
            credentials.clientSecret ?: map.getFirst(OAuth2ParameterNames.CLIENT_SECRET),
            map.getFirst(PkceParameterNames.CODE_VERIFIER)
        )
    }

    private suspend fun extractClientAuthenticationData(request: ServerRequest): ClientCredentials {
        var encodedClientAuth = ""
        val authorizationHeader: String? = request.headers().firstHeader("Authorization")
        var clientId: String? = null
        var clientSecret: String? = null

        if (authorizationHeader?.isNotBlank() == true && "BASIC".equals(authorizationHeader.substring(0, 5), ignoreCase = true)) {
            encodedClientAuth = authorizationHeader.substring(6)
            var position: Int = encodedClientAuth.indexOf(':')
            val plainClientAuth = if (position == -1) {
                //base64 encoded
                decodeBase64(encodedClientAuth)
            } else {
                encodedClientAuth
            }

            position = plainClientAuth.indexOf(':')

            if (position != -1) {
                clientId = plainClientAuth.substring(0, position)
                clientSecret = plainClientAuth.substring(position + 1)
            } else {
                throw OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.ACCESS_DENIED, "Invalid Authorization header", ""))
            }
        }

        return ClientCredentials(clientId, clientSecret);
    }

    private inline fun decodeBase64(credentials: String): String {
        return String(Base64.getDecoder().decode(credentials), StandardCharsets.UTF_8)
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
        @field:JsonProperty("client_secret")
        val clientSecret: String?,
        @field:JsonProperty("code_verifier")
        val codeVerifier: String?,
    )

    data class ClientCredentials(
        val clientId: String?,
        val clientSecret: String?
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
