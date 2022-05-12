package hu.nagygm.oauth2.server.handler

import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.server.ServerResponse
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

public suspend fun respondWithError(error: OAuth2Error): ServerResponse {
    val parameters = mapOf<String, String>(
        OAuth2ParameterNames.ERROR to error.errorCode,
        OAuth2ParameterNames.ERROR_URI to error.uri,
        OAuth2ParameterNames.ERROR_DESCRIPTION to error.description
    )
    return ServerResponse.badRequest().bodyValue(parameters).awaitFirst()
}

public suspend fun redirectWithError(error: OAuth2Error, redirectUri: String, state: String?): ServerResponse {
    val parameters = LinkedMultiValueMap<String,String>()
    parameters.add(OAuth2ParameterNames.ERROR, urlEncodeValue(error.errorCode))
    parameters.add(OAuth2ParameterNames.ERROR_URI, urlEncodeValue(error.uri))
    parameters.add(OAuth2ParameterNames.ERROR_DESCRIPTION, urlEncodeValue(error.description))
    if(state != null) {parameters.add(OAuth2ParameterNames.STATE, state)}
    return ServerResponse.status(HttpStatus.FOUND)
        .location(URI(buildLocation(redirectUri, parameters))).build().awaitFirst()
}

public fun urlEncodeValue(value : String) = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

public suspend fun buildLocation(redirectUri: String, parameters: MultiValueMap<String, String>): String {
    val uri = URI(redirectUri)
    val query = uri.query
    val queryParams =
        if (query.isNullOrEmpty()) {
            parameters
        }
        else {
            parameters.toSingleValueMap().plus(query.split("&").map { it.split("=") }.map { it[0] to it[1] })
        }
    val queryString = queryParams.map { "${it.key}=${it.value}" }.joinToString("&")
    return "${uri.scheme}://${uri.host}${uri.path}?$queryString"
}