package hu.nagygm.oauth2.server.exception

import hu.nagygm.oauth2.server.handler.AuthorizationHandler
import hu.nagygm.oauth2.server.handler.AuthorizationHandler.AuthorizationRequest


interface GeneralContext {
    fun <V> put(key: Class<V>, value: Any)
    fun <V> get(key: Class<V>): V?
}

class SimpleMapBasedContext : GeneralContext {
    val context: MutableMap<Class<*>, Any> = mutableMapOf()

    override fun <V> put(key: Class<V>, value: Any) {
        context[key] = value
    }

    override fun <V> get(key: Class<V>): V? {
        context[key]?.let {
            return it as V
        }
        return null
    }
}

interface RequestExceptionContext<T> {
    fun getRequest() : T
}

class AuthorizationRequestContext(private val request: AuthorizationRequest, private val exception: Throwable) : RequestExceptionContext<AuthorizationRequest> {
    override fun getRequest() = request
}

