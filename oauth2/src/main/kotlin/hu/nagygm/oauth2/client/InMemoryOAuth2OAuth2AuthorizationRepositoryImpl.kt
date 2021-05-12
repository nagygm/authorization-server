package hu.nagygm.oauth2.client

import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
open class InMemoryOAuth2OAuth2AuthorizationRepositoryImpl : OAuth2AuthorizationRepository {
    private val authorizations: MutableMap<String, OAuth2Authorization> = ConcurrentHashMap()

    override suspend fun findById(id: String): OAuth2Authorization? {
        return authorizations[id]
    }

    override suspend fun save(authorization: OAuth2Authorization) {
        authorizations[authorization.id] = authorization
    }

    override suspend fun remove(id: String) {
        authorizations.remove(id)
    }
}
