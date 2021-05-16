package hu.nagygm.oauth2.server

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

class Oauth2AuthenticationToken(authorities: MutableCollection<out GrantedAuthority>?) :
    AbstractAuthenticationToken(authorities) {
    override fun getCredentials(): Any {
        TODO("Implement this for more spring security integration")
    }

    override fun getPrincipal(): Any {
        TODO("Implement this for more spring security integration")
    }
}
