package hu.nagygm.oauth2.server.security.springsecurity

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

class OAuth2AuthenticationToken(authorities: MutableCollection<out GrantedAuthority>?) :
    AbstractAuthenticationToken(authorities) {
    override fun getCredentials(): Any {
        TODO("Implement this for more spring security integration")
    }

    override fun getPrincipal(): Any {
        TODO("Implement this for more spring security integration")
    }
}