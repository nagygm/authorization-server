package hu.nagygm.oauth2.client

import org.springframework.security.oauth2.core.AbstractOAuth2Token
import org.springframework.security.oauth2.core.AuthorizationGrantType

data class OAuth2Authorization(
    val id: String,
    val registeredClient: ClientRegistration,
    val principal: String,
    val grantType: AuthorizationGrantType,
    val tokens: Set<AbstractOAuth2Token>,
    val attributes: Map<String, Any>
)
