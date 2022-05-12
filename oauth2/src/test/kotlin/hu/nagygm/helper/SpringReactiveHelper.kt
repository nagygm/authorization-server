package hu.nagygm.helper

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.MediaType
import org.springframework.http.codec.HttpMessageWriter
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.HandlerStrategies
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.result.view.ViewResolver
import reactor.core.publisher.Mono
import java.net.URLEncoder
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


open class RawGetRequestObjectMother<T : RawGetRequestObjectMother<T>> {
    private lateinit var path: String
    private val paramMap: MultiValueMap<String, String?> = LinkedMultiValueMap()

    fun withPath(path: String): RawGetRequestObjectMother<T> {
        this.path = path
        return this
    }

    fun addQueryParamList(name: String, values: List<String>): RawGetRequestObjectMother<T> {
        paramMap.addAll(name, values)
        return this
    }

    fun addQueryParamList(name: String, vararg values: String): RawGetRequestObjectMother<T> {
        paramMap.addAll(name, values.toList())
        return this
    }

    fun addQueryParam(name: String, value: String?): RawGetRequestObjectMother<T> {
        paramMap.add(name, value)
        return this
    }

    fun removeQueryParam(name: String) : RawGetRequestObjectMother<T> {
        paramMap.remove(name)
        return this
    }

    fun replaceQueryParam(name: String, value: String?): RawGetRequestObjectMother<T> {
        paramMap.remove(name)
        paramMap.add(name, value)
        return this
    }

    fun build(): MockServerHttpRequest = MockServerHttpRequest.get(path)
        .queryParams(paramMap)
        .build()

}

open class RawPostRequestObjectMother<T : RawPostRequestObjectMother<T>> {
    private lateinit var path: String
    private val formBody: MultiValueMap<String, String?> = LinkedMultiValueMap()

    fun withPath(path: String): RawPostRequestObjectMother<T> {
        this.path = path
        return this
    }

    fun addFormBodyValueList(name: String, values: List<String>): RawPostRequestObjectMother<T> {
        formBody.addAll(name, values)
        return this
    }

    fun addFormBodyValueList(name: String, vararg values: String): RawPostRequestObjectMother<T> {
        formBody.addAll(name, values.toList())
        return this
    }

    fun addFormBodyValue(name: String, value: String?): RawPostRequestObjectMother<T> {
        formBody.add(name, value)
        return this
    }

    fun removeFormBodyValue(name: String) : RawPostRequestObjectMother<T> {
        formBody.remove(name)
        return this
    }

    fun replaceFormBodyValue(name: String, value: String?): RawPostRequestObjectMother<T> {
        formBody.remove(name)
        formBody.add(name, value)
        return this
    }

    fun build(): MockServerHttpRequest {
        val body = formBody.formMapToString()
        return MockServerHttpRequest.post(path)
            .contentLength(body.toByteArray(Charsets.UTF_8).size.toLong())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(body)
    }
}

fun MultiValueMap<String,String?>.formMapToString() : String {
    val map = this
    return map.map { (key, list) ->
        list.map { "${URLEncoder.encode(key, Charsets.UTF_8)}=${URLEncoder.encode(it, Charsets.UTF_8)}"
        }.joinToString { "&" }
    }.joinToString { "&" }
}


