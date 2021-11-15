package hu.nagygm.oauth2.server

import hu.nagygm.oauth2.client.registration.ClientRegistrationRepository
import hu.nagygm.oauth2.client.OAuth2AuthorizationRepository
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication

/**
 * Implementation of [AuthenticationProvider] that supports OAuth2.
 * This makes the spring 
 */
open class OAuth2AuthenticationProvider(
    private val clientRegistrationRepository: ClientRegistrationRepository,
    private val oAuth2AuthorizationRepository: OAuth2AuthorizationRepository
) : AuthenticationProvider {

    override fun authenticate(authentication: Authentication?): Authentication {
        checkNotNull(authentication) { "Authentication must not be null!" }
        TODO("add client login repository for machine and ip authorization, Implement provider to use spring securit")
    }

    override fun supports(authentication: Class<*>?): Boolean {
        TODO("Not yet implemented")
    }

}
