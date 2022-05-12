package hu.nagygm.oauth2.server.client

import hu.nagygm.oauth2.server.client.registration.ClientRegistration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.stereotype.Repository
import java.util.*
import java.util.concurrent.ConcurrentHashMap

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

class DefaultOAuth2AuthorizationFactoryImpl : OAuth2AuthorizationFactory {
    override fun create(
        registeredClient: ClientRegistration,
        principal: String,
        grantType: AuthorizationGrantType,
        refreshToken: OAuth2RefreshToken?,
        accessToken: OAuth2AccessToken,
        attributes: Map<String, Any>
    ): OAuth2Authorization {
        return OAuth2AuthorizationImpl(
            UUID.randomUUID().toString(),
            registeredClient,
            principal,
            grantType,
            refreshToken,
            accessToken,
            attributes
        )
    }

}

data class OAuth2AuthorizationImpl(
    override val id: String,
    override val registeredClient: ClientRegistration,
    override val principal: String,
    override val grantType: AuthorizationGrantType,
    override val refreshToken: OAuth2RefreshToken?,
    override val accessToken: OAuth2AccessToken,
    override val attributes: Map<String, Any>
) : OAuth2Authorization
