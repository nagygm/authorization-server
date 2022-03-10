package hu.nagygm.helper

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.codec.HttpMessageWriter
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.reactive.function.server.HandlerStrategies
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.result.view.ViewResolver
import java.util.*

fun ServerResponse.fetchBodyAsString(serverWebExchange: MockServerWebExchange): String {
    this.writeTo(serverWebExchange, DEFAULT_CONTEXT).block()
    return serverWebExchange.response.bodyAsString.block()!!
}

fun ServerResponse.fetchBodyAsJsonNode(serverWebExchange: MockServerWebExchange): JsonNode {
    this.writeTo(serverWebExchange, DEFAULT_CONTEXT).block()
    return testObjectMapper.readTree(serverWebExchange.response.bodyAsString.block())
}

val DEFAULT_CONTEXT: ServerResponse.Context = object : ServerResponse.Context {
    override fun messageWriters(): List<HttpMessageWriter<*>> {
        return HandlerStrategies.withDefaults().messageWriters()
    }

    override fun viewResolvers(): List<ViewResolver> {
        return Collections.emptyList()
    }
}

val testObjectMapper: ObjectMapper = ObjectMapper()




