package hu.nagygm.oauth2.util


interface TypeSafeMap<K : TypeSafeMap.Key<T, *>, T> {
    interface Key<T, V> {
        fun key(): T
    }
}

abstract class AbstractTypeSafeMap<K : TypeSafeMap.Key<T, *>, T>(private val map: MutableMap<T, Any>) :
    TypeSafeMap<K, T> {

    @Suppress("UNCHECKED_CAST")
    fun <E> get(key: TypeSafeMap.Key<T, E>): E {
        return map[key.key()] as E
    }

    @Suppress("UNCHECKED_CAST")
    fun <E : Any> put(key: TypeSafeMap.Key<T, E>, value: E): E? {
        return map.put(key.key(), value) as E?
    }
}
