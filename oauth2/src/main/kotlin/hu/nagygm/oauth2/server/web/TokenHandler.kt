package hu.nagygm.oauth2.server.web

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyAndAwait

@Service
open class TokenHandler {
    suspend fun acquireToken(request: ServerRequest): ServerResponse = dummy(request)
    suspend fun revokeToken(request: ServerRequest): ServerResponse = dummy(request)

    private suspend fun dummy(request: ServerRequest): ServerResponse =
        ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON).bodyAndAwait(flow {
                delay(1000L)
                emit(1)
            })
}

