package hu.nagygm.oauth2.client

/**
 * Authorization server client
 */
data class ClientRegistration(
    val id: String,
    val clientId: String,
    val secret: String,
    val redirectUris: Set<String>,
    val authorizationGrantTypes:
)
