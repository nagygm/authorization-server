package hu.nagygm.oauth2.server.handler

import hu.nagygm.oauth2.server.service.ConsentService
import hu.nagygm.oauth2.util.LoggerDelegate
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitFormData
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Service
class ConsentHandler(
    @Autowired val consentService: ConsentService
) {
    private val log by LoggerDelegate()

    suspend fun handleConsent(serverRequest: ServerRequest): ServerResponse {
        val form = serverRequest.awaitFormData()

        val response = consentService.processConsent(
            form.getFirst("id") ?: "",
            form.getFirst("consentAccepted")?.toBooleanStrictOrNull() ?: false,
            form.getFirst("acceptedScopes")?.split("&")?.toSet() ?: emptySet()
        )
        return ServerResponse.status(HttpStatus.FOUND)
            .location(URI.create(response.redirectUri)).build().awaitFirst()
    }


    private suspend fun redirectWithError(error: OAuth2Error, redirectUri: String, state: String?): ServerResponse {
        val parameters = LinkedMultiValueMap<String, String>()
        parameters.add(OAuth2ParameterNames.ERROR, urlEncodeValue(error.errorCode))
        parameters.add(OAuth2ParameterNames.ERROR_URI, urlEncodeValue(error.uri))
        parameters.add(OAuth2ParameterNames.ERROR_DESCRIPTION, urlEncodeValue(error.description))
        if (state != null) {
            parameters.add(OAuth2ParameterNames.STATE, state)
        }
        return ServerResponse.status(HttpStatus.FOUND)
            .location(URI(buildLocation(redirectUri, parameters))).build().awaitFirst()
    }

    private fun urlEncodeValue(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

    private suspend fun buildLocation(redirectUri: String, parameters: MultiValueMap<String, String>): String {
        val uri = URI(redirectUri)
        val query = uri.query
        val queryParams =
            if (query.isNullOrEmpty()) {
                parameters
            } else {
                parameters.toSingleValueMap().plus(query.split("&").map { it.split("=") }.map { it[0] to it[1] })
            }
        val queryString = queryParams.map { "${it.key}=${it.value}" }.joinToString("&")
        return "${uri.scheme}://${uri.host}${uri.path}?$queryString"
    }
}