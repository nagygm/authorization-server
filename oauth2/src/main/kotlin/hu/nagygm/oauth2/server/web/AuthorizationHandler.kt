package hu.nagygm.oauth2.server.web

import hu.nagygm.oauth2.client.registration.ClientRegistration
import hu.nagygm.oauth2.client.registration.ClientRegistrationRepository
import hu.nagygm.oauth2.core.Endpoint
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.mono
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.oauth2.core.*
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.stereotype.Service
import org.springframework.util.MultiValueMap
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyAndAwait
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import java.net.MalformedURLException
import java.net.URL


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
open class AuthorizationHandler(@Autowired var clientRegistrationRepository: ClientRegistrationRepository) {

    suspend fun authorize(serverRequest: ServerRequest): ServerResponse {
        val request = requestToAuthorizationRequest(serverRequest)
        try {
            AuthorizationRequest.AuthorizationRequestValidator(clientRegistrationRepository).validate(request)
        } catch (ex: OAuth2AuthorizationException) {
            return ServerResponse.badRequest().body(mono { ex.error }, OAuth2Error::class.java).awaitFirst()
        }
        //redirect to consent page
        //if not logged in redirect to login page using security then back to consent page listing scopes
        //if consent approved redirect with code and state to client redirect uri
        //if error or consent denied redirect to client uri with error or access denied
        //if redirect uri invalid or not registered then show error on consent page

        //if consent added save consent for listed scopes and client
        //save login data for later use
        //issue id and access token as site only cookie for authorization server
        //issue 
        return ServerResponse.status(HttpStatus.FOUND).header("Location", "/consent").build().awaitFirst()
    }

    private fun requestToAuthorizationRequest(request: ServerRequest): AuthorizationRequest {
        return AuthorizationRequest(request.queryParams())
    }

    class AuthorizationRequest(parameters: MultiValueMap<String, String>) {
        val clientId: String = parameters.getFirst(OAuth2ParameterNames.CLIENT_ID) ?: ""
        val state: String? = parameters.getFirst(OAuth2ParameterNames.STATE)
        val responseType: String = parameters.getFirst(OAuth2ParameterNames.RESPONSE_TYPE) ?: ""
        val redirectUri: String = parameters.getFirst(OAuth2ParameterNames.REDIRECT_URI) ?: ""
        val scopes: Set<String>

        init {

            val inputScopes = parameters.getFirst(OAuth2ParameterNames.SCOPE)

            scopes = if (inputScopes != null && inputScopes.isNotEmpty()) {
                inputScopes.split(" ").toSet()
            } else {
                emptySet()
            }
        }

        class AuthorizationRequestValidator(private val clientRegistrationRepository: ClientRegistrationRepository) {
            fun supports(clazz: Class<*>): Boolean =
                clazz.isAssignableFrom(AuthorizationRequest::class.java)

            suspend fun validate(target: AuthorizationRequest) {

                //----- VALIDATE RESPONSE TYPE TODO extract to business logic validator
                if (target.responseType.isBlank()) {
                    throw OAuth2AuthorizationException(
                        OAuth2Error(
                            OAuth2ErrorCodes.INVALID_REQUEST,
                            "Invalid parameter: ${OAuth2ParameterNames.RESPONSE_TYPE}",
                            ""
                        )
                    )
                } else if (SupportedResponseTypesRegistry.notContains(target.responseType)) {
                    throw OAuth2AuthorizationException(
                        OAuth2Error(
                            OAuth2ErrorCodes.UNSUPPORTED_RESPONSE_TYPE,
                            "Unsupported response type: ${target.responseType}",
                            ""
                        )
                    )
                }
                //----- VALIDATE client ID TODO extract to business logic validator

                if (target.clientId.isBlank()) {
                    throw OAuth2AuthorizationException(
                        OAuth2Error(
                            OAuth2ErrorCodes.INVALID_REQUEST,
                            "Invalid parameter: ${OAuth2ParameterNames.CLIENT_ID}",
                            ""
                        )
                    )
                }

                val registration = clientRegistrationRepository.findByClientId(target.clientId)
                    ?: throw OAuth2AuthorizationException(
                        OAuth2Error(
                            OAuth2ErrorCodes.UNAUTHORIZED_CLIENT,
                            "Client not authorized: ${target.clientId}",
                            ""
                        )
                    )
                registration.authorizationGrantTypes.any {
                    it == SupportedResponseTypesRegistry.responseTypeToGrantType(target.responseType)
                }

                //----- VALIDATE REDIRECT URI TODO extract to business logic validator
                if ((registration.redirectUris.size != 1 && target.redirectUri.isBlank()) || (target.redirectUri.isNotBlank() &&
                            (!validateUri(target.redirectUri) || !registration.redirectUris.contains(target.redirectUri)))
                ) {
                    throw OAuth2AuthorizationException(
                        OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST)
                    )
                }
            }

            private fun validateUri(uri: String): Boolean {
                try {
                    URL(uri)
                } catch (e: MalformedURLException) {
                    return false
                }
                return true
            }

            object SupportedResponseTypesRegistry {
                val responseTypes: Map<String, AuthorizationGrantType> = hashMapOf(
                    OAuth2AuthorizationResponseType.CODE.value to AuthorizationGrantType.AUTHORIZATION_CODE,
                    OAuth2AuthorizationResponseType.TOKEN.value to AuthorizationGrantType.IMPLICIT
                )

                suspend fun contains(responseType: String?): Boolean {
                    return responseTypes.contains(responseType)
                }

                suspend fun notContains(responseType: String?): Boolean {
                    return !responseTypes.contains(responseType)
                }

                suspend fun responseTypeToGrantType(responseType: String?): AuthorizationGrantType? {
                    return responseTypes[responseType]
                }
            }
        }
    }
}
