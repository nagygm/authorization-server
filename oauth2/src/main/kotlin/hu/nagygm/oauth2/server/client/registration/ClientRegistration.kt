package hu.nagygm.oauth2.server.client.registration

import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType

/**
 * Authorization server client
 */
data class ClientRegistration(
    val id: String,
    val clientId: String,
    val secret: String,
    val redirectUris: Set<String>,
    val authorizationGrantTypes: Set<AuthorizationGrantType>,
    val scopes: Set<String>,
    val clientConfiguration: Map<ClientConfigurationParams, Any>,
    val tokenEndpointAuthMethod: TokenEndpointAuthMethod,
    val type: ClientType = ClientType.PUBLIC
)

/**
 * RFC7591 Dynamic Client Registration Protocol Data
 */
data class ClientMetadata(
    val clientId: String,
    val redirectUris: Set<String>,
    val tokenEndpointAuthMethod: TokenEndpointAuthMethod,
    val grantTypes: Set<AuthorizationGrantType>,
    val responseTypes: Set<OAuth2AuthorizationResponseType>,
    val clientName: String,
    val clientUri: String,
)

enum class TokenEndpointAuthMethod(val value: String) {
    /**
     * RFC7591 Section 2.0
     * The client is a public client as defined in OAuth 2.0,
     * Section 2.1, and does not have a client secret.
     */
    NONE("none"),
    /**
     * RFC7591 Section 2.0
     * The client is a public client as defined in OAuth 2.0,
     * Section 2.1, and does not have a client secret.
     */
    CLIENT_SECRET_POST("client_secret_post"),

    /**
     * RFC7591 Section 2.0
     * The client uses the HTTP POST parameters
     * as defined in OAuth 2.0, Section 2.3.1.
     */
    CLIENT_SECRET_BASIC("client_secret_basic"),

    /**
     * OPENID Connect Core 1.0
     * https://www.iana.org/assignments/oauth-parameters/oauth-parameters.xhtml#token-endpoint-auth-method
     */
    CLIENT_SECRET_JWT("client_secret_jwt"),

    /**
     * OPENID Connect Core 1.0
     * https://www.iana.org/assignments/oauth-parameters/oauth-parameters.xhtml#token-endpoint-auth-method
     */
    PRIVATE_KEY_JWT("private_key_jwt"),

    /**
     * RFC8705 Section 2.1.1
     * https://www.rfc-editor.org/rfc/rfc8705.html
     */
    TLS_CLIENT_AUTH("tls_client_auth"),

    /**
     * RFC8705 Section 2.2.1
     * https://www.rfc-editor.org/rfc/rfc8705.html
     */
    SELF_SIGNED_TLS_CLIENT_AUTH("self_signed_tls_client_auth");

    /**
     * If unspecified or omitted, the default is "client_secret_basic"
     * RFC7591 Section 2.0 page 8
     */
    fun getDefaultValue(): TokenEndpointAuthMethod {
        return CLIENT_SECRET_BASIC
    }

    fun getByValue(value: String) : TokenEndpointAuthMethod? {
        return values().find { it.value == value }
    }
}

enum class ClientType(val value: String) {
    PUBLIC("public"),
    CREDENTIALED("credentialed"),
    CONFIDENTIAL("confidential");

    fun getByValue(value: String) : ClientType? {
        return values().find { it.value == value }
    }
}