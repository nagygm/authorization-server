package hu.nagygm.oauth2.client

import org.springframework.security.oauth2.core.AuthorizationGrantType

/**
 * Authorization server client
 */
data class ClientRegistration(
    val id: String,
    val clientId: String,
    val secret: String,
    val redirectUris: Set<String>,
    val authorizationGrantTypes: Set<AuthorizationGrantType>
)
