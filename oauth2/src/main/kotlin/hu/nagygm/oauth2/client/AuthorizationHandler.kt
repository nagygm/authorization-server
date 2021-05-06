package hu.nagygm.oauth2.client

import kotlinx.coroutines.flow.flow
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyAndAwait


@Service
open class AuthorizationHandler {
    suspend fun echoNumber(request: ServerRequest): ServerResponse =
        ServerResponse.ok()
            .contentType(MediaType.APPLICATION_NDJSON).bodyAndAwait(flow {
                kotlinx.coroutines.delay(1000)
                emit(1)
            })
}
