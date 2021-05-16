package hu.nagygm.oauth2.server

import hu.nagygm.oauth2.client.registration.ClientRegistrationRepository
import hu.nagygm.oauth2.client.OAuth2AuthorizationRepository
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication

open class OAuth2AuthenticationProvider(
    private val clientRegistrationRepository: ClientRegistrationRepository,
    private val oAuth2AuthorizationRepository: OAuth2AuthorizationRepository
) : AuthenticationProvider {

    override fun authenticate(authentication: Authentication?): Authentication {
        checkNotNull(authentication) { "Authentication must not be null!" }
        TODO("add client login repository for machine and ip authorization")
        TODO("Implement provider to use spring security")
    }

    override fun supports(authentication: Class<*>?): Boolean {
        TODO("Not yet implemented")
    }

}
