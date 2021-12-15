package hu.nagygm.oauth2.client

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class InMemoryOAuth2AuthorizationRepositoryImpl : OAuth2AuthorizationRepository {
    private val authorizations: MutableMap<String, MutableMap<String,OAuth2Authorization>> = ConcurrentHashMap()

    override suspend fun findByIdAndClientId(id: String, clientId: String): OAuth2Authorization? {
        return authorizations[clientId]?.get("id")
    }

    override suspend fun save(authorization: OAuth2Authorization) {
        if(authorizations[authorization.registeredClient.clientId] == null) {
            authorizations[authorization.registeredClient.clientId] = mutableMapOf(authorization.id to authorization)
        } else {
            //TODO: prevent uuid collision
            authorizations[authorization.registeredClient.clientId]?.put(authorization.id, authorization)
        }
    }

    override suspend fun remove(id: String, clientId: String) {
        authorizations[clientId]?.remove(id)
    }

    override suspend fun findByRefreshToken(refreshToken: OAuth2RefreshToken, clientId: String): OAuth2Authorization? {
       return authorizations[clientId].let {
            it?.values?.first { auth ->
                auth.refreshToken == refreshToken
            }
        }
    }
}
