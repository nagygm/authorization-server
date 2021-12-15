package hu.nagygm.oauth2.client

import hu.nagygm.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.AbstractOAuth2Token
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2RefreshToken

data class OAuth2Authorization(
    val id: String,
    val registeredClient: ClientRegistration,
    val principal: String,
    val grantType: AuthorizationGrantType,
    val refreshToken: OAuth2RefreshToken?,
    val accessToken: OAuth2AccessToken,
    val attributes: Map<String, Any>
)
