package hu.nagygm.oauth2.client.registration

import hu.nagygm.oauth2.util.AbstractTypeSafeMap
import hu.nagygm.oauth2.util.TypeSafeMap

class ClientConfigurationMap() :
    AbstractTypeSafeMap<ClientConfigurationParamKeys<*>, String>(HashMap()) {
}

sealed class ClientConfigurationParamKeys<V>(val key: String) : TypeSafeMap.Key<String, V> {
    override fun key(): String {
        return key
    }

    object AccessTokenLifetime : ClientConfigurationParamKeys<Long>("access-token-lifetime")
    object RefreshTokenLifetime : ClientConfigurationParamKeys<Long>("refresh-token-lifetime")
}

enum class ClientConfigurationParams(val path: String) {
    ACCESS_TOKEN_LIFETIME("access-token-lifetime"),
    REFRESH_TOKEN_LIFETIME("refresh-token-lifetime"),
}
