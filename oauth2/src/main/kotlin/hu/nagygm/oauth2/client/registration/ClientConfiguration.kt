package hu.nagygm.oauth2.client.registration

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class ClientConfiguration(private val parameters: ConcurrentMap<ClientConfigurationParams, Any> = ConcurrentHashMap()) {
    fun get(key: String): Any? {
        require(key.isNotEmpty()) { "Parameter key must not be empty" }
        return parameters[key]
    }

    fun put(key: ClientConfigurationParams, value: Any): ClientConfiguration {
        parameters[key] = value
        return this
    }

    /**
     * Should be generated
     */
    fun accessTokenLifetime(): Int {
        return parameters[ClientConfigurationParams.ACCESS_TOKEN_LIFETIME] as Int
    }

    /**
     * Should be generated
     */
    fun refreshTokenLifeTime(): Int {
        return parameters[ClientConfigurationParams.REFRESH_TOKEN_LIFETIME] as Int
    }
}

enum class ClientConfigurationParams(val path: String) {
    ACCESS_TOKEN_LIFETIME("access-token-lifetime"),
    REFRESH_TOKEN_LIFETIME("refresh-token-lifetime"),
}
