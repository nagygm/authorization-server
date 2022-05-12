package hu.nagygm.oauth2.server.client

import hu.nagygm.oauth2.server.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2RefreshToken


interface OAuth2AuthorizationRepository {
    suspend fun findByIdAndClientId(id: String, clientId: String): OAuth2Authorization?
    suspend fun save(authorization: OAuth2Authorization)
    suspend fun remove(id: String, clientId: String)
    suspend fun findByRefreshToken(refreshToken: OAuth2RefreshToken, clientId: String): OAuth2Authorization?
}

interface OAuth2Authorization {
    val id: String
    val registeredClient: ClientRegistration
    val principal: String
    val grantType: AuthorizationGrantType
    val refreshToken: OAuth2RefreshToken?
    val accessToken: OAuth2AccessToken
    val attributes: Map<String, Any>
}

interface OAuth2AuthorizationFactory {
    fun create(registeredClient: ClientRegistration,
               principal: String,
               grantType: AuthorizationGrantType,
               refreshToken: OAuth2RefreshToken?,
               accessToken: OAuth2AccessToken,
               attributes: Map<String, Any>): OAuth2Authorization
}