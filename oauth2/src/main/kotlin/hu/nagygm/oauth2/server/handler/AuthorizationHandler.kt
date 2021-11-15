package hu.nagygm.oauth2.server.handler

import hu.nagygm.oauth2.client.registration.*
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.mono
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.core.*
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.stereotype.Service
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
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
open class AuthorizationHandler(
    @Autowired val clientRegistrationRepository: ClientRegistrationRepository,
    @Autowired val grantRequestService: GrantRequestService
) {

    suspend fun authorize(serverRequest: ServerRequest): ServerResponse {
        val request = requestToAuthorizationRequest(serverRequest)
        val grantRequest = try {
            AuthorizationRequest.AuthorizationRequestValidator(clientRegistrationRepository, grantRequestService).validate(request)
        } catch (ex: OAuth2AuthorizationException) {
            return ServerResponse.badRequest().body(mono { ex.error }, OAuth2Error::class.java).awaitFirst()
        }
        val location = "/consent?grant_request_id=${grantRequest.id}&client_id=${grantRequest.clientId}"

        return ServerResponse.status(HttpStatus.FOUND).header("Location", location).build().awaitFirst()
    }

    private suspend fun requestToAuthorizationRequest(request: ServerRequest): AuthorizationRequest {
        val result = AuthorizationRequest(request.queryParams())
        val headers: ServerRequest.Headers = request.headers()
        val cache = request.exchange().session.cache().awaitFirst()
        cache.attributes
        result.clientSecret = headers.firstHeader("Authorization") ?:"" //TODO fix  not sending client secre,t spring security?
        return result
    }

    class AuthorizationRequest(parameters: MultiValueMap<String, String>) {
        val clientId: String = parameters.getFirst(OAuth2ParameterNames.CLIENT_ID) ?: ""
        val state: String? = parameters.getFirst(OAuth2ParameterNames.STATE)
        val responseType: String = parameters.getFirst(OAuth2ParameterNames.RESPONSE_TYPE) ?: ""
        var redirectUri: String = parameters.getFirst(OAuth2ParameterNames.REDIRECT_URI) ?: ""
        val scopes: Set<String>
        var clientSecret: String = ""

        init {

            val inputScopes = parameters.getFirst(OAuth2ParameterNames.SCOPE)

            scopes = if (inputScopes != null && inputScopes.isNotEmpty()) {
                inputScopes.split(" ").toSet()
            } else {
                emptySet()
            }
        }

        class AuthorizationRequestValidator(
            private val clientRegistrationRepository: ClientRegistrationRepository,
            private val grantRequestService: GrantRequestService
        ) {
            fun supports(clazz: Class<*>): Boolean =
                clazz.isAssignableFrom(AuthorizationRequest::class.java)

            suspend fun validate(authorizationRequest: AuthorizationRequest): GrantRequest {

                //----- VALIDATE RESPONSE TYPE TODO extract to business logic validator
                if (authorizationRequest.responseType.isBlank()) {
                    throw OAuth2AuthorizationException(
                        OAuth2Error(
                            OAuth2ErrorCodes.INVALID_REQUEST,
                            "Invalid parameter: ${OAuth2ParameterNames.RESPONSE_TYPE}",
                            ""
                        )
                    )
                } else if (SupportedResponseTypesRegistry.notContains(authorizationRequest.responseType)) {
                    throw OAuth2AuthorizationException(
                        OAuth2Error(
                            OAuth2ErrorCodes.UNSUPPORTED_RESPONSE_TYPE,
                            "Unsupported response type: ${authorizationRequest.responseType}",
                            ""
                        )
                    )
                }
                //----- VALIDATE client ID TODO extract to business logic validator

                if (authorizationRequest.clientId.isBlank()) {
                    throw OAuth2AuthorizationException(
                        OAuth2Error(
                            OAuth2ErrorCodes.INVALID_REQUEST,
                            "Invalid parameter: ${OAuth2ParameterNames.CLIENT_ID}",
                            ""
                        )
                    )
                }

                val registration =
                    clientRegistrationRepository.findByClientIdAndSecret(authorizationRequest.clientId, authorizationRequest.clientSecret)
                        ?: throw OAuth2AuthorizationException(
                            OAuth2Error(
                                OAuth2ErrorCodes.UNAUTHORIZED_CLIENT,
                                "Client not authorized: ${authorizationRequest.clientId}",
                                ""
                            )
                        )
                if(!registration.authorizationGrantTypes.any {
                    it == SupportedResponseTypesRegistry.responseTypeToGrantType(authorizationRequest.responseType)
                }) {
                    throw OAuth2AuthorizationException(
                        OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT)
                    )
                }

                //----- VALIDATE REDIRECT URI TODO extract to business logic validator
                if ((registration.redirectUris.size != 1 && authorizationRequest.redirectUri.isBlank()) || (authorizationRequest.redirectUri.isNotBlank() &&
                            (!validateUri(authorizationRequest.redirectUri) || !registration.redirectUris.contains(authorizationRequest.redirectUri)))
                ) {
                    throw OAuth2AuthorizationException(
                        OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST)
                    )
                }

                //----- VALIDATE scopes TODO extract to business logic validator
                if (!registration.scopes.containsAll(authorizationRequest.scopes)) {
                    throw OAuth2AuthorizationException(
                        OAuth2Error(OAuth2ErrorCodes.INVALID_SCOPE)
                    )
                }


                if (authorizationRequest.redirectUri.isBlank()) authorizationRequest.redirectUri = registration.redirectUris.first()
                return grantRequestService.saveGrantRequest(authorizationRequest)
            }

            private fun validateUri(uri: String): Boolean {
                try {
                    URL(uri)
                } catch (e: MalformedURLException) {
                    return false
                }
                return true
            }

            //TODO to util class
            object SupportedResponseTypesRegistry {
                val responseTypes: Map<String, AuthorizationGrantType> = hashMapOf(
                    OAuth2AuthorizationResponseType.CODE.value to AuthorizationGrantType.AUTHORIZATION_CODE,
//                    OAuth2AuthorizationResponseType.TOKEN.value to AuthorizationGrantType.IMPLICIT
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
    }
}
