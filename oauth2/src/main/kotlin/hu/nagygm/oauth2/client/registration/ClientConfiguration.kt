package hu.nagygm.oauth2.client.registration

class ClientConfigurationMap() :
    AbstractTypeSafeMap<ClientConfigurationParamKeys<*>, String>(HashMap()) {
}

interface TypeSafeMap<K : TypeSafeMap.Key<T, *>, T> {
    interface Key<T, V> {
        fun key(): T
    }
}

abstract class AbstractTypeSafeMap<K : TypeSafeMap.Key<T, *>, T>(private val map: MutableMap<T, Any>) : TypeSafeMap<K, T> {

    @Suppress("UNCHECKED_CAST")
    fun <E> get(key: TypeSafeMap.Key<T, E>): E {
        return map[key.key()] as E
    }

    @Suppress("UNCHECKED_CAST")
    fun <E : Any> put(key: TypeSafeMap.Key<T, E>, value: E): E? {
        return map.put(key.key(), value) as E?
    }
}


sealed class ClientConfigurationParamKeys<V>(val key: String) : TypeSafeMap.Key<String, V> {
    override fun key(): String {
        return key
    }

    object AccessTokenLifetime : ClientConfigurationParamKeys<Int>("access-token-lifetime")
    object RefreshTokenLifetime : ClientConfigurationParamKeys<Int>("refresh-token-lifetime")
}

enum class ClientConfigurationParams(val path: String) {
    ACCESS_TOKEN_LIFETIME("access-token-lifetime"),
    REFRESH_TOKEN_LIFETIME("refresh-token-lifetime"),
}
