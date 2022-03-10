package hu.nagygm.oauth2.server.handler

import hu.nagygm.oauth2.client.registration.*
import hu.nagygm.oauth2.config.annotation.OAuth2AuthorizationServerEndpointConfiguration.OAuth2Api
import hu.nagygm.oauth2.server.GrantRequest
import hu.nagygm.oauth2.server.GrantRequestService
import hu.nagygm.oauth2.server.security.pkce.CodeChallengeMethods
import hu.nagygm.oauth2.server.security.pkce.CodeVerifier
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.mono
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.core.*
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.util.regex.Matcher


/**
Authorization Endpoint

The authorization endpoint is used to interact with the resource
owner and obtain an authorization grant.  The authorization server
MUST first verify the identity of the resource owner.  The way in
which the authorization server authenticates the resource owner
(e.g., username and password login, session cookies) is beyond the
scope of this specification.

The means through which the client obtains the location of the
authorization endpoint are beyond the scope of this specification,
but the location is typically provided in the service documentation.

The endpoint URI MAY include an "application/x-www-form-urlencoded"
formatted (per Appendix B) query component ([RFC3986] Section 3.4),
which MUST be retained when adding additional query parameters.  The
endpoint URI MUST NOT include a fragment component.

Since requests to the authorization endpoint result in user
authentication and the transmission of clear-text credentials (in the
HTTP response), the authorization server MUST require the use of TLS
as described in Section 1.6 when sending requests to the
authorization endpoint.

The authorization server MUST support the use of the HTTP "GET"
method [RFC2616] for the authorization endpoint and MAY support the
use of the "POST" method as well.

Parameters sent without a value MUST be treated as if they were
omitted from the request.  The authorization server MUST ignore
unrecognized request parameters.  Request and response parameters
MUST NOT be included more than once.
 */
@Service
class AuthorizationHandler(
    @Autowired val clientRegistrationRepository: ClientRegistrationRepository,
    @Autowired val grantRequestService: GrantRequestService
) {

    suspend fun authorize(serverRequest: ServerRequest): ServerResponse {
        val request = AuthorizationRequest(serverRequest.queryParams())
        val result = AuthorizationRequest.AuthorizationRequestValidator(clientRegistrationRepository).validate(request)
        return if (result.isValid) {
            val grantRequest = grantRequestService.saveGrantRequest(request)
            //TODO move consent url to configuration
            val location = "${OAuth2Api.oauth2}${OAuth2Api.consent}?grant_request_id=${grantRequest.id}&client_id=${grantRequest.clientId}"

            ServerResponse.status(HttpStatus.FOUND).header("Location", location).build().awaitFirst()
            
        } else {
            if(result.error == null) {
                throw IllegalStateException("Invalid validation")
            } else if(result.isRedirectable ){
                //TODO proper redriect uri
                redirectWithError(result.error, "", LinkedMultiValueMap())
            } else {
                respondWithError(result.error, request)
            }
        }

    }

    private suspend fun respond(grantRequest: GrantRequest) :ServerResponse {
        //TODO if configuration for clients requires consent, redirect to consent page else redirect to redirect url
        val location = "${OAuth2Api.oauth2}${OAuth2Api.consent}?grant_request_id=${grantRequest.id}&client_id=${grantRequest.clientId}"
        return ServerResponse.status(HttpStatus.FOUND).header("Location", location).build().awaitFirst()
    }

    private suspend fun respondWithError(error: OAuth2Error, request: AuthorizationRequest): ServerResponse {
        return ServerResponse.badRequest().bodyValue(error).awaitFirst()
    }

    private suspend fun redirectWithError(error: OAuth2Error, redirectUri: String, parameters: MultiValueMap<String, String>): ServerResponse {
        //when has valid redirect URI must send
        return ServerResponse.status(HttpStatus.FOUND)
            .location(URI(buildLocation(redirectUri, parameters))).build().awaitFirst()
    }
    private suspend fun buildLocation(redirectUri: String, parameters: MultiValueMap<String, String>): String {
        val url = URL(redirectUri)
        val query = url.query
        val queryParams =
            if (query.isNullOrEmpty()) {
                parameters
            }
            else {
                parameters.toSingleValueMap().plus(query.split("&").map { it.split("=") }.map { it[0] to it[1] })
            }
        val queryString = queryParams.map { "${it.key}=${it.value}" }.joinToString("&")
        return "${url.protocol}://${url.host}${url.path}?$queryString"
    }

    class AuthorizationRequest(parameters: MultiValueMap<String, String>) {
        val clientId: String
        val state: String?
        val responseType: String
        var redirectUri: String
        val scopes: Set<String>
        val codeChallenge: String?
        val codeChallengeMethod: String?

        init {
            clientId = parameters.getFirst(OAuth2ParameterNames.CLIENT_ID) ?: ""
            state = parameters.getFirst(OAuth2ParameterNames.STATE)
            responseType = parameters.getFirst(OAuth2ParameterNames.RESPONSE_TYPE) ?: ""
            redirectUri = parameters.getFirst(OAuth2ParameterNames.REDIRECT_URI) ?: ""
            codeChallenge = parameters.getFirst(PkceParameterNames.CODE_CHALLENGE)
            codeChallengeMethod = parameters.getFirst(PkceParameterNames.CODE_CHALLENGE_METHOD)

            val inputScopes = parameters.getFirst(OAuth2ParameterNames.SCOPE)

            scopes = if (inputScopes != null && inputScopes.isNotEmpty()) {
                inputScopes.split(" ").toSet()
            } else {
                emptySet()
            }
        }

        //TODO refactore to validator/result/context type class hierarchy, commander or specification?
        class AuthorizationRequestValidator(
            private val clientRegistrationRepository: ClientRegistrationRepository
        ) {
            fun supports(clazz: Class<*>): Boolean =
                clazz.isAssignableFrom(AuthorizationRequest::class.java)

            suspend fun validate(authorizationRequest: AuthorizationRequest): AuthorizationValidationResult {

                if (!isCodeChallengeValid(authorizationRequest.codeChallenge, authorizationRequest.codeChallengeMethod)) {
                    return AuthorizationValidationResult( error =
                        OAuth2Error(
                            OAuth2ErrorCodes.INVALID_REQUEST,
                            "Code challenge is invalid",
                            "https://datatracker.ietf.org/doc/html/draft-ietf-oauth-v2-1-04#section-4.1.2.1"
                        )
                    )
                }

                //----- VALIDATE RESPONSE TYPE TODO extract to business logic validator
                if (authorizationRequest.responseType.isBlank()) {
                    return AuthorizationValidationResult( error =
                    OAuth2Error(
                            OAuth2ErrorCodes.INVALID_REQUEST,
                            "Invalid parameter: ${OAuth2ParameterNames.RESPONSE_TYPE}",
                            "https://datatracker.ietf.org/doc/html/draft-ietf-oauth-v2-1-04#section-4.1.2.1"
                        )
                    )
                } else if (SupportedResponseTypesRegistry.notContains(authorizationRequest.responseType)) {
                    return AuthorizationValidationResult( error =
                    OAuth2Error(
                            OAuth2ErrorCodes.UNSUPPORTED_RESPONSE_TYPE,
                            "Unsupported response type: ${authorizationRequest.responseType}",
                            "https://datatracker.ietf.org/doc/html/draft-ietf-oauth-v2-1-04#section-4.1.2.1"
                        )
                    )
                }
                //----- VALIDATE client ID TODO extract to business logic validator

                if (authorizationRequest.clientId.isBlank()) {
                    return AuthorizationValidationResult( error =
                    OAuth2Error(
                            OAuth2ErrorCodes.INVALID_REQUEST,
                            "Invalid parameter: ${OAuth2ParameterNames.CLIENT_ID}",
                            "https://datatracker.ietf.org/doc/html/draft-ietf-oauth-v2-1-04#section-4.1.2.1"
                        )
                    )
                }

                //TODO add property for authorized client flows
                val registration =
                    clientRegistrationRepository.findByClientId(authorizationRequest.clientId)
                        ?: return AuthorizationValidationResult(
                            error = OAuth2Error(
                                OAuth2ErrorCodes.UNAUTHORIZED_CLIENT,
                                "Client not authorized: ${authorizationRequest.clientId}",
                                "https://datatracker.ietf.org/doc/html/draft-ietf-oauth-v2-1-04#section-4.1.2.1"
                            )
                        )
                if (!registration.authorizationGrantTypes.any {
                        it == SupportedResponseTypesRegistry.responseTypeToGrantType(authorizationRequest.responseType)
                    }) {
                    return AuthorizationValidationResult( error =
                        OAuth2Error(
                            OAuth2ErrorCodes.UNAUTHORIZED_CLIENT,
                            "Client is not to authorize with this response type",
                            "https://datatracker.ietf.org/doc/html/draft-ietf-oauth-v2-1-04#section-4.1.2.1"
                        )
                    )
                }

                //----- VALIDATE REDIRECT URI TODO extract to business logic validator
                if ((registration.redirectUris.size != 1 && authorizationRequest.redirectUri.isBlank()) || (authorizationRequest.redirectUri.isNotBlank() &&
                            (!validateUri(authorizationRequest.redirectUri) || !registration.redirectUris.contains(authorizationRequest.redirectUri)))
                ) {
                    return AuthorizationValidationResult(error =
                        OAuth2Error(
                            OAuth2ErrorCodes.INVALID_REQUEST,
                            "Invalid redirect URI, or redirect URI not registered for client",
                            "https://datatracker.ietf.org/doc/html/draft-ietf-oauth-v2-1-04#section-4.1.2.1"
                        )
                    )
                }

                //----- VALIDATE scopes TODO extract to business logic validator
                if (authorizationRequest.scopes.isEmpty() && !registration.scopes.containsAll(authorizationRequest.scopes)) {
                    return AuthorizationValidationResult( error =
                        OAuth2Error(OAuth2ErrorCodes.INVALID_SCOPE,
                            "Scopes not matching the clients scopes",
                            "https://datatracker.ietf.org/doc/html/draft-ietf-oauth-v2-1-04#section-4.1.2.1"
                        )
                    )
                }

                return AuthorizationValidationResult(
                    isValid = true, isRedirectable = true, error = null
                )
            }


            private fun validateUri(uri: String): Boolean {
                try {
                    URL(uri)
                } catch (e: MalformedURLException) {
                    return false
                }
                return true
            }

            private fun isCodeChallengeValid(codeChallenge: String?, codeChallengeMethod: String?): Boolean {
                if (codeChallenge == null) {
                    // As for oauth2.1 draft recommendation sha256 code_challenge should be mandatory
                    return true
                } else {
                    if (codeChallengeMethod != null) {
                        // https://tools.ietf.org/html/rfc7636#section-4.2
                        if (!CodeChallengeMethods.isValid(codeChallengeMethod)) {
                            throw OAuth2AuthorizationException(
                                OAuth2Error(
                                    OAuth2ErrorCodes.INVALID_REQUEST,
                                    "Invalid parameter: ${PkceParameterNames.CODE_CHALLENGE_METHOD}",
                                    ""
                                )
                            )
                        }
                    }

                    // check the format and length of the code_challenge
                    val matcher: Matcher = CodeVerifier.CODE_CHALLENGE_PATTERN.matcher(codeChallenge);
                    if (matcher.matches()) {
                        return true
                    }
                    return false
                }
            }

            //TODO to util class
            object SupportedResponseTypesRegistry {
                private val responseTypes: Map<String, AuthorizationGrantType> = hashMapOf(
                    OAuth2AuthorizationResponseType.CODE.value to AuthorizationGrantType.AUTHORIZATION_CODE,
                )

                fun contains(responseType: String): Boolean {
                    return responseTypes.contains(responseType)
                }

                fun notContains(responseType: String): Boolean {
                    return !responseTypes.contains(responseType)
                }

                fun responseTypeToGrantType(responseType: String): AuthorizationGrantType? {
                    return responseTypes[responseType]
                }
            }
        }
        data class AuthorizationValidationResult(
            val isValid: Boolean = false,
            val isRedirectable: Boolean = false,
            val error: OAuth2Error?
        )
    }
}


