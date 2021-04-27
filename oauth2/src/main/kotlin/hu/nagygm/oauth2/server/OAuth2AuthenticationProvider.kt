package hu.nagygm.oauth2.server

import hu.nagygm.oauth2.client.ClientRegistrationRepository
import hu.nagygm.oauth2.client.OAuth2AuthorizationRepository
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication

open class OAuth2AuthenticationProvider(
    val clientRegistrationRepository: ClientRegistrationRepository,
    val oAuth2AuthorizationRepository: OAuth2AuthorizationRepository
) : AuthenticationProvider {

    override fun authenticate(authentication: Authentication?): Authentication {
        TODO("add client login repository for machine and ip authorization")
        TODO("Not yet implemented")
    }

    override fun supports(authentication: Class<*>?): Boolean {
        TODO("Not yet implemented")
    }

}
