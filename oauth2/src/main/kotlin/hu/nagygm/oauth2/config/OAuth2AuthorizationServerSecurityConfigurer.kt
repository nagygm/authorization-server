package hu.nagygm.oauth2.config

import org.springframework.security.config.annotation.web.HttpSecurityBuilder
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer

/**
 * Future class for congfiguring OAuth2 server properties for a spring authoriztaion server
 */
class OAuth2AuthorizationServerSecurityConfigurer<B : HttpSecurityBuilder<B>>
    : AbstractHttpConfigurer<OAuth2AuthorizationServerSecurityConfigurer<B>, B>() {


}
