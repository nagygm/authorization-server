package hu.nagygm.oauth2.server.handler

import hu.nagygm.oauth2.server.client.registration.*
import hu.nagygm.oauth2.config.OAuth2Api
import hu.nagygm.oauth2.server.service.GrantRequest
import hu.nagygm.oauth2.server.service.GrantRequestService
import hu.nagygm.oauth2.server.exception.AuthorizationCodeGrantException
import hu.nagygm.oauth2.server.security.pkce.CodeChallengeMethods
import hu.nagygm.oauth2.server.security.pkce.CodeVerifier
import hu.nagygm.oauth2.server.validators.RedirectUriValidator
import hu.nagygm.oauth2.util.LoggerDelegate
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.core.*
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames
import org.springframework.stereotype.Service
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
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
formatted (per Appendix B) query component (RFC3986 Section 3.4),
which MUST be retained when adding additional query parameters.  The
endpoint URI MUST NOT include a fragment component.

Since requests to the authorization endpoint result in user
authentication and the transmission of clear-text credentials (in the
HTTP response), the authorization server MUST require the use of TLS
as described in Section 1.6 when sending requests to the
authorization endpoint.

The authorization server MUST support the use of the HTTP "GET"
method RFC2616 for the authorization endpoint and MAY support the
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
    @Autowired
    lateinit var oauth2Api: OAuth2Api


    private val log by LoggerDelegate()

    suspend fun authorize(serverRequest: ServerRequest): ServerResponse {
        //TODO refactor to sane try catching
        val request = try {
            AuthorizationRequest.create(serverRequest.queryParams())
        } catch (ex: AuthorizationCodeGrantException) {
            log.info("Authorization Code grant exception error code: {}, description: {}", { ex.error.errorCode }, { ex.error.description })
            log.debug(ex)
            return respondWithError(ex.error)
        }
        val result = try {
            AuthorizationRequest.AuthorizationRequestValidator(clientRegistrationRepository).validate(request)
        } catch (ex: AuthorizationCodeGrantException) {
            log.info("Authorization Code grant exception error code: {}, description: {}", { ex.error.errorCode }, { ex.error.description })
            log.debug(ex)
            AuthorizationRequest.AuthorizationValidationResult(error = ex.error)
        }
        return if (result.isValid) {
            val grantRequest = grantRequestService.saveGrantRequest(request)
            respond(grantRequest)
        } else {
            if (result.error == null) {
                throw IllegalStateException("Invalid validation")
            } else if (result.isRedirectable) {
                redirectWithError(result.error, request.redirectUri, request.state)
            } else {
                respondWithError(result.error)
            }
        }

    }

    private suspend fun respond(grantRequest: GrantRequest): ServerResponse {
        val location = "${oauth2Api.oauth2}${oauth2Api.consent}?grant_request_id=${grantRequest.id}&client_id=${grantRequest.clientId}"
        return ServerResponse.status(HttpStatus.FOUND).header("Location", location).build().awaitFirst()
    }

    class AuthorizationRequest private constructor(
        val clientId: String,
        val state: String?,
        val responseType: String,
        var redirectUri: String,
        val scopes: Set<String>,
        val codeChallenge: String?,
        val codeChallengeMethod: String?
    ) {

        companion object {
            private val parameterNames: Set<String> = setOf(
                OAuth2ParameterNames.CLIENT_ID, OAuth2ParameterNames.STATE, OAuth2ParameterNames.RESPONSE_TYPE,
                OAuth2ParameterNames.REDIRECT_URI, PkceParameterNames.CODE_CHALLENGE, PkceParameterNames.CODE_CHALLENGE_METHOD,
                OAuth2ParameterNames.SCOPE
            )

            fun create(parameters: MultiValueMap<String, String>): AuthorizationRequest {
                val inputScopes = parameters.getFirst(OAuth2ParameterNames.SCOPE)
                val authorizationRequest = AuthorizationRequest(
                    clientId = parameters.getFirst(OAuth2ParameterNames.CLIENT_ID) ?: "",
                    state = parameters.getFirst(OAuth2ParameterNames.STATE),
                    responseType = parameters.getFirst(OAuth2ParameterNames.RESPONSE_TYPE) ?: "",
                    redirectUri = parameters.getFirst(OAuth2ParameterNames.REDIRECT_URI) ?: "",
                    codeChallenge = parameters.getFirst(PkceParameterNames.CODE_CHALLENGE),
                    codeChallengeMethod = parameters.getFirst(PkceParameterNames.CODE_CHALLENGE_METHOD),
                    scopes = if (inputScopes != null && inputScopes.isNotEmpty()) {
                        inputScopes.split(" ").toSet()
                    } else {
                        emptySet()
                    }
                )
                if (parameters.any { parameterNames.contains(it.key) && it.value.size > 1 }) {
                    throw AuthorizationCodeGrantException(
                        OAuth2Error(
                            OAuth2ErrorCodes.INVALID_REQUEST,
                            "Duplicated parameters: ${OAuth2ParameterNames.CLIENT_ID}",
                            AuthorizationCodeGrantException.oauth2DocumentationURI
                        ), authorizationRequest
                    )
                }

                return authorizationRequest
            }
        }


        //TODO refactor to validator/result/context type class hierarchy, commander or specification?
        class AuthorizationRequestValidator(
            private val clientRegistrationRepository: ClientRegistrationRepository
        ) {
            fun supports(clazz: Class<*>): Boolean =
                clazz.isAssignableFrom(AuthorizationRequest::class.java)

            suspend fun validate(authorizationRequest: AuthorizationRequest): AuthorizationValidationResult {

                //----- VALIDATE client ID TODO extract to business logic validator
                if (authorizationRequest.clientId.isBlank()) {
                    return AuthorizationValidationResult(
                        error =
                        OAuth2Error(
                            OAuth2ErrorCodes.INVALID_REQUEST,
                            "Invalid parameter: ${OAuth2ParameterNames.CLIENT_ID}",
                            AuthorizationCodeGrantException.oauth2DocumentationURI
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
                                AuthorizationCodeGrantException.oauth2DocumentationURI
                            )
                        )

                //----- VALIDATE REDIRECT URI TODO extract to business logic validator
                if (RedirectUriValidator.validate(
                        authorizationRequest.redirectUri,
                        registration.redirectUris
                    )
                ) return AuthorizationValidationResult(
                    error =
                    OAuth2Error(
                        OAuth2ErrorCodes.INVALID_REQUEST,
                        "Invalid redirect URI, or redirect URI not registered for client",
                        AuthorizationCodeGrantException.oauth2DocumentationURI
                    )
                )

                if (!isCodeChallengeValid(authorizationRequest.codeChallenge, authorizationRequest.codeChallengeMethod)) {
                    return AuthorizationValidationResult(
                        error =
                        OAuth2Error(
                            OAuth2ErrorCodes.INVALID_REQUEST,
                            "Code challenge is invalid",
                            AuthorizationCodeGrantException.oauth2DocumentationURI
                        )
                    )
                }

                //----- VALIDATE RESPONSE TYPE TODO extract to business logic validator
                if (authorizationRequest.responseType.isBlank()) {
                    return AuthorizationValidationResult(
                        error =
                        OAuth2Error(
                            OAuth2ErrorCodes.INVALID_REQUEST,
                            "Invalid parameter: ${OAuth2ParameterNames.RESPONSE_TYPE}",
                            AuthorizationCodeGrantException.oauth2DocumentationURI
                        )
                    )
                } else if (SupportedResponseTypesRegistry.notContains(authorizationRequest.responseType)) {
                    return AuthorizationValidationResult(
                        isRedirectable = true, error =
                        OAuth2Error(
                            OAuth2ErrorCodes.UNSUPPORTED_RESPONSE_TYPE,
                            "Unsupported response type: ${authorizationRequest.responseType}",
                            AuthorizationCodeGrantException.oauth2DocumentationURI
                        )
                    )
                }

                if (!registration.authorizationGrantTypes.any {
                        it == SupportedResponseTypesRegistry.responseTypeToGrantType(authorizationRequest.responseType)
                    }) {
                    return AuthorizationValidationResult(
                        isRedirectable = true, error =
                        OAuth2Error(
                            OAuth2ErrorCodes.UNAUTHORIZED_CLIENT,
                            "Client is not to authorize with this response type",
                            AuthorizationCodeGrantException.oauth2DocumentationURI
                        )
                    )
                }

                //----- VALIDATE scopes TODO extract to business logic validator
                if (authorizationRequest.scopes.isEmpty() && !registration.scopes.containsAll(authorizationRequest.scopes)) {
                    return AuthorizationValidationResult(
                        isRedirectable = true, error =
                        OAuth2Error(
                            OAuth2ErrorCodes.INVALID_SCOPE,
                            "Scopes not matching the clients scopes",
                            AuthorizationCodeGrantException.oauth2DocumentationURI
                        )
                    )
                }

                return AuthorizationValidationResult(
                    isValid = true, isRedirectable = true, error = null
                )
            }


            private fun isCodeChallengeValid(codeChallenge: String?, codeChallengeMethod: String?): Boolean {
                if (codeChallenge == null) {
                    // As for oauth2.1 draft recommendation sha256 code_challenge should be mandatory
                    return true
                } else {
                    // https://tools.ietf.org/html/rfc7636#section-4.2
                    if (codeChallengeMethod != null && !CodeChallengeMethods.isValid(codeChallengeMethod)) {
                        throw OAuth2AuthorizationException(
                            OAuth2Error(
                                OAuth2ErrorCodes.INVALID_REQUEST,
                                "Invalid parameter: ${PkceParameterNames.CODE_CHALLENGE_METHOD}",
                                ""
                            )
                        )
                    }

                    // check the format and length of the code_challenge
                    val matcher: Matcher = CodeVerifier.CODE_CHALLENGE_PATTERN.matcher(codeChallenge)
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


