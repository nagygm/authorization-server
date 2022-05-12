package hu.nagygm.oauth2.config.default

import hu.nagygm.oauth2.config.OAuth2Api
import hu.nagygm.oauth2.config.OAuth2AuthorizationServerEndpointConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter
import org.springframework.security.web.server.util.matcher.*

/**
 * Default security configuration for the authorization server endpoints
 */
@Configuration
class DefaultOAuth2ServerSecurityConfiguration {

    @Autowired lateinit var oauth2Api: OAuth2Api

    @ConditionalOnProperty(prefix = "oauth2.security.default.oauth2", name = ["enabled"])
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @Bean
    fun oauth2DefaultServerSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http
            .csrf().requireCsrfProtectionMatcher(NegatedServerWebExchangeMatcher { exchange ->
                ServerWebExchangeMatchers.matchers(
                    oauth2RequestMatcher(), oauth2ConsentRequestMatcher()
                ).matches(exchange)
            }).and()
            .authorizeExchange()
            .matchers(oauth2RequestMatcher()) //TODO temp fix till more integrated spring security, until that manual auth inside the handlers
            .permitAll()
            .and().formLogin()
            .loginPage("/login")
            .authenticationSuccessHandler(redirectSuccessHandler()).and().logout()
            .and().headers().referrerPolicy()
            .policy(ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.ORIGIN_WHEN_CROSS_ORIGIN)
        return http.build()
    }



    /**
     * Needed for redirection after login
     */
    private fun redirectSuccessHandler(): RedirectServerAuthenticationSuccessHandler {
        return RedirectServerAuthenticationSuccessHandler()
    }

    @Bean("oauth2RequestMatcher")
    fun oauth2RequestMatcher(): ServerWebExchangeMatcher {
        val authorizationEndpointMatcher = OrServerWebExchangeMatcher(
            PathPatternParserServerWebExchangeMatcher(oauth2Api.authorizePath(), HttpMethod.POST),
            PathPatternParserServerWebExchangeMatcher(oauth2Api.authorizePath(), HttpMethod.GET)
        )
        val tokenEndpointMatcher =
            PathPatternParserServerWebExchangeMatcher(oauth2Api.tokenPath(), HttpMethod.POST)
        val tokenRevocationEndpointMatcher =
            PathPatternParserServerWebExchangeMatcher(oauth2Api.revocationPath(), HttpMethod.POST)

        return OrServerWebExchangeMatcher(
            authorizationEndpointMatcher, tokenEndpointMatcher, tokenRevocationEndpointMatcher
        )
    }

    @Bean("oauth2ConsentRequestMatcher")
    fun oauth2ConsentRequestMatcher(): ServerWebExchangeMatcher {
        return PathPatternParserServerWebExchangeMatcher(oauth2Api.consentPath(), HttpMethod.GET)
    }
    
}

