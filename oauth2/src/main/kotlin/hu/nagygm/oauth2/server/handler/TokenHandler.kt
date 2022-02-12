package hu.nagygm.oauth2.server.handler

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
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
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.*
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames
import org.springframework.stereotype.Service
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.server.*
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*
import java.util.regex.Matcher

//TODO extract client authentication validations
//TODO extract error throwing into separate method, to manage redirect and non redirect types

@Service
class TokenHandler(
    @Autowired val clientRegistrationRepository: ClientRegistrationRepository,
    @Autowired val grantRequestService: GrantRequestService,
    @Autowired val oAuth2AuthorizationRepository: OAuth2AuthorizationRepository,
    @Autowired @Qualifier("tokenJsonMapper") val mapper: ObjectMapper,
    @Autowired val passwordEncoder: PasswordEncoder,
    @Value("\${oauth2.jwt.secret}") var jwtSecretKey: String
) {

    companion object {
        private val logger = LogFactory.getLog(TokenHandler::class.java)
    }

    private val codeGenerator = Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 32)

    suspend fun acquireToken(request: ServerRequest): ServerResponse {
        val response = validate(request)
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
            .headers { it["Cache-Control"] = "no-store"; it["Pragma"] = "no-cache" }
            .bodyValue(response).awaitFirst()
    }

    suspend fun revokeToken(request: ServerRequest): ServerResponse = TODO("Implement revocation")

    private suspend fun validate(request: ServerRequest): TokenResponse {
        val map = request.awaitFormData()
        val credentials = extractClientAuthenticationData(request)
        return when (map.getFirst(OAuth2ParameterNames.GRANT_TYPE).lowercase()) {
            AuthorizationGrantType.AUTHORIZATION_CODE.value.lowercase() -> {
                validateAuthorizationGrantType(formToAuthorizationCodeTokenRequest(map, credentials))
            }
            AuthorizationGrantType.CLIENT_CREDENTIALS.value.lowercase() -> {
                validateClientCredentialsGrantType(formToClientCredentialsTokenRequest(map, credentials))
            }
            AuthorizationGrantType.REFRESH_TOKEN.value.lowercase() -> {
                validateRefreshTokenGrantType(formToRefreshTokenRequest(map, credentials))
            }
            else -> {
                throw OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.UNSUPPORTED_GRANT_TYPE))
            }
        }
    }

    private suspend fun validateRefreshTokenGrantType(request: RefreshTokenRequest): TokenResponse {
        if (request.refreshToken.isNullOrBlank()) {
            throw OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST))
        }
        val oauth2RefreshToken = fromPayloadToRefreshToken(jwtDecoder(request.refreshToken))

        val clientRegistration = if (request.clientId.isNullOrBlank()) {
            throw OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST))
        } else {
            clientRegistrationRepository.findByClientId(request.clientId)
                ?: throw OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT))
        }
        if (clientRegistration.secret.isNotEmpty() && !passwordEncoder.matches(request.clientSecret, clientRegistration.secret)) {
            throw OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT))
        }
        if (!clientRegistration.authorizationGrantTypes.contains(AuthorizationGrantType.REFRESH_TOKEN)) {
            throw OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT))
        }
        val authorization = oAuth2AuthorizationRepository.findByRefreshToken(oauth2RefreshToken, clientRegistration.clientId)
            ?: throw OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.ACCESS_DENIED))

        val tokenResponse = generateToken(
            clientRegistration,
            authorization.accessToken.scopes,
            true,
            authorization.principal,
            AuthorizationGrantType.REFRESH_TOKEN
        )
        oAuth2AuthorizationRepository.remove(authorization.id, clientRegistration.clientId)
        return tokenResponse
    }

    private suspend fun validateAuthorizationGrantType(request: AuthorizationCodeTokenRequest): TokenResponse {
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

            if (!validateCodeChallenge(grantRequest, request)) {
                throw OAuth2AuthorizationException(
                    OAuth2Error(
                        OAuth2ErrorCodes.INVALID_REQUEST,
                        "Invalid code_challenge",
                        "https://datatracker.ietf.org/doc/html/rfc7636#section-4.4.1"
                    )
                )
            }

            if (Instant.now().isBefore(grantRequest.codeCreatedAt?.plusSeconds(600L))) {
                return generateToken(
                    clientRegistration,
                    grantRequest.acceptedScopes,
                    true,
                    grantRequest.associatedUserId!!,
                    AuthorizationGrantType.AUTHORIZATION_CODE
                )
            } else {
                throw (OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT)))
            }
        } else {
            throw (OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT)))
        }
    }

    private fun validateCodeChallenge(grantRequest: GrantRequest, authorizationCodeTokenRequest: AuthorizationCodeTokenRequest): Boolean {
        val codeVerifier = authorizationCodeTokenRequest.codeVerifier
        if (grantRequest.codeChallenge != null && grantRequest.codeChallengeMethod != null) {
            val matcher: Matcher = CodeVerifier.CODE_CHALLENGE_PATTERN.matcher(grantRequest.codeChallenge);
            if (authorizationCodeTokenRequest.codeVerifier == null || authorizationCodeTokenRequest.codeVerifier.isEmpty() || !matcher.matches()) {
                return false
            }
            return when (grantRequest.codeChallengeMethod!!.lowercase()) {
                CodeChallengeMethods.S256.value -> {
                    val digestedCodeChallenge: String = CodeVerifier.processCodeChallenge(authorizationCodeTokenRequest.codeVerifier)
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

    private suspend fun validateClientCredentialsGrantType(request: ClientCredentialsTokenRequest): TokenResponse {
        if (request.clientId.isNullOrBlank() || request.clientSecret.isNullOrBlank()) {
            throw     OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT))
        }

        val clientRegistration = clientRegistrationRepository.findByClientId(request.clientId)
        var scopes = request.scope?.split(" ")?.toSet()
        if (clientRegistration != null) {
            if (clientRegistration.secret.isNotEmpty() && !passwordEncoder.matches(request.clientSecret, clientRegistration.secret)) {
                throw OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.ACCESS_DENIED, "Invalid Authorization header", ""))
            }
            if (!clientRegistration.authorizationGrantTypes.contains(AuthorizationGrantType.CLIENT_CREDENTIALS)) {
                throw OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT))
            }
            if (scopes == null) {
                scopes = clientRegistration.scopes
            } else {
                if (clientRegistration.scopes.containsAll(scopes)) {
                    throw OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.INVALID_SCOPE, "Scoeps not matching", ""))
                }
            }
            return generateToken(clientRegistration, scopes, false, clientRegistration.clientId, AuthorizationGrantType.CLIENT_CREDENTIALS)
        } else {
            throw OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT, "Invalid Client Authorization", ""))
        }
    }

    //TODO refactor too many parameters
    private suspend fun generateToken(
        clientRegistration: ClientRegistration,
        scopes: Set<String>,
        includeRefreshToken: Boolean,
        principal: String,
        grantType: AuthorizationGrantType
    ): TokenResponse {

        val now = Instant.now()
        val accessToken = OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            codeGenerator.generateKey(),
            now,
            now.plusSeconds(
                (clientRegistration.clientConfiguration[ClientConfigurationParams.ACCESS_TOKEN_LIFETIME] as Int).toLong()
            ),
            scopes
        )
        //TODO refactor this to nicer implementation
        val refreshToken = if (includeRefreshToken) {
            OAuth2RefreshToken(
                codeGenerator.generateKey(),
                now,
                now.plusSeconds(
                    (clientRegistration.clientConfiguration[ClientConfigurationParams.REFRESH_TOKEN_LIFETIME] as Int).toLong()
                ),
            )
        } else {
            null
        }

        val token = OAuth2Authorization(
            UUID.randomUUID().toString(), //TODO move to repository
            clientRegistration,
            principal,
            grantType,
            refreshToken,
            accessToken,
            emptyMap()
        )
        oAuth2AuthorizationRepository.save(
            token
        )
        return TokenResponse(
            jwtEncoder(accessToken),
            OAuth2AccessToken.TokenType.BEARER.value,
            3600,
            scopes.reduce { s1, s2 -> "$s1 $s2" },
            if (includeRefreshToken && refreshToken != null) jwtEncoder(refreshToken) else null
        )
    }

    private fun jwtEncoder(token: AbstractOAuth2Token): String {
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        val jwsObject = JWSObject(
            JWSHeader(JWSAlgorithm.HS256),
            Payload(mapper.writeValueAsString(token))
        )

        val sharedKey = jwtSecretKey.toByteArray()

        jwsObject.sign(MACSigner(sharedKey))
        return jwsObject.serialize(false)
    }

    private fun jwtDecoder(token: String): Payload {
        val jwsObject = JWSObject.parse(token)
        val sharedKey = jwtSecretKey.toByteArray()
        val verifier = MACVerifier(sharedKey)
        jwsObject.verify(verifier)
        return jwsObject.payload
    }

    private fun fromPayloadToRefreshToken(payload: Payload): OAuth2RefreshToken {
        val token = mapper.readValue(payload.toString(), object : TypeReference<HashMap<String, String>>() {})
        return OAuth2RefreshToken(
            token["tokenValue"] as String,
            Instant.parse(token["issuedAt"]),
            Instant.parse(token["expiresAt"])
        )
    }

    //TODO refactor to validators and converters
    private suspend fun formToAuthorizationCodeTokenRequest(
        map: MultiValueMap<String, String>,
        credentials: ClientCredentials
    ): AuthorizationCodeTokenRequest {
        return AuthorizationCodeTokenRequest(
            map.getFirst(OAuth2ParameterNames.GRANT_TYPE),
            map.getFirst(OAuth2ParameterNames.CODE),
            map.getFirst(OAuth2ParameterNames.REDIRECT_URI),
            credentials.clientId ?: map.getFirst(OAuth2ParameterNames.CLIENT_ID),
            credentials.clientSecret ?: map.getFirst(OAuth2ParameterNames.CLIENT_SECRET),
            map.getFirst(PkceParameterNames.CODE_VERIFIER)
        )
    }

    private suspend fun formToClientCredentialsTokenRequest(
        map: MultiValueMap<String, String>,
        credentials: ClientCredentials
    ): ClientCredentialsTokenRequest {
        return ClientCredentialsTokenRequest(
            map.getFirst(OAuth2ParameterNames.GRANT_TYPE),
            credentials.clientId ?: map.getFirst(OAuth2ParameterNames.CLIENT_ID),
            map.getFirst(OAuth2ParameterNames.SCOPE),
            credentials.clientSecret ?: map.getFirst(OAuth2ParameterNames.CLIENT_SECRET),
        )
    }

    private suspend fun formToRefreshTokenRequest(map: MultiValueMap<String, String>, credentials: ClientCredentials): RefreshTokenRequest {
        return RefreshTokenRequest(
            map.getFirst(OAuth2ParameterNames.GRANT_TYPE),
            map.getFirst(OAuth2ParameterNames.REFRESH_TOKEN),
            credentials.clientId ?: map.getFirst(OAuth2ParameterNames.CLIENT_ID),
            map.getFirst(OAuth2ParameterNames.SCOPE),
            credentials.clientSecret ?: map.getFirst(OAuth2ParameterNames.CLIENT_SECRET),
        )
    }

    private suspend fun extractClientAuthenticationData(request: ServerRequest): ClientCredentials {
        val authorizationHeader: String? = request.headers().firstHeader("Authorization")
        var clientId: String? = null
        var clientSecret: String? = null

        if (authorizationHeader?.isNotBlank() == true && "BASIC".equals(authorizationHeader.substring(0, 5), ignoreCase = true)) {
            var encodedClientAuth = authorizationHeader.substring(6)
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

    private fun decodeBase64(credentials: String): String {
        return String(Base64.getDecoder().decode(credentials), StandardCharsets.UTF_8)
    }

    data class AuthorizationCodeTokenRequest(
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

    data class ClientCredentialsTokenRequest(
        @field:JsonProperty("grant_type")
        val grantType: String?,
        @field:JsonProperty("client_id")
        val clientId: String?,
        @field:JsonProperty("scope")
        val scope: String?,
        @field:JsonProperty("client_secret")
        val clientSecret: String?,
    )

    data class RefreshTokenRequest(
        @field:JsonProperty("grant_type")
        val grantType: String?,
        @field:JsonProperty("refresh_token")
        val refreshToken: String?,
        @field:JsonProperty("client_id")
        val clientId: String?,
        @field:JsonProperty("scope")
        val scope: String?,
        @field:JsonProperty("client_secret")
        val clientSecret: String?,
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
        @field:JsonProperty("scope")
        val scope: String,
        @field:JsonProperty("refresh_token")
        val refreshToken: String?
    )
}
