package hu.nagygm.oauth2.client

import org.springframework.security.oauth2.core.OAuth2RefreshToken


interface OAuth2AuthorizationRepository {

    suspend fun findByIdAndClientId(id: String, clientId: String): OAuth2Authorization?

    suspend fun save(authorization: OAuth2Authorization)

    suspend fun remove(id: String, clientId: String)

    suspend fun findByRefreshToken(refreshToken: OAuth2RefreshToken, clientId: String): OAuth2Authorization?
}
