package hu.nagygm.oauth2.config.default

import hu.nagygm.oauth2.config.annotation.OAuth2AuthorizationServerEndpointConfiguration
import hu.nagygm.oauth2.core.Endpoint
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
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
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers

/**
 * Default security configuration for the authorization server endpoints
 */
@Configuration
class DefaultOAuth2ServerSecurityConfiguration {

    @ConditionalOnProperty(prefix = "oauth2.security.default", name = ["enabled"])
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @Bean
    fun serverSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http
            .csrf().requireCsrfProtectionMatcher(NegatedServerWebExchangeMatcher { exchange ->
                ServerWebExchangeMatchers.pathMatchers(
                    "${OAuth2AuthorizationServerEndpointConfiguration.oauth2}/**", "/login**", "${OAuth2AuthorizationServerEndpointConfiguration.consentPath()}",
                ).matches(exchange)
            }).and()
            .authorizeExchange()
            .pathMatchers("${OAuth2AuthorizationServerEndpointConfiguration.authorizePath()}", "${OAuth2AuthorizationServerEndpointConfiguration.tokenPath()}", "${OAuth2AuthorizationServerEndpointConfiguration.consentPath()}", "/login", )
            .permitAll()
            .and().formLogin()
            .loginPage("/login")
            .authenticationSuccessHandler(redirectSuccessHandler()).and().logout()
            .and().headers().referrerPolicy()
            .policy(ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.ORIGIN_WHEN_CROSS_ORIGIN)

        return http.authorizeExchange().matchers(RequestMatcherHelper.requestMatchers).authenticated().and().build()
    }

    private fun redirectSuccessHandler(): RedirectServerAuthenticationSuccessHandler {
        return RedirectServerAuthenticationSuccessHandler()
    }

    

}

/**
 * Defines request matchers for server
 */
object RequestMatcherHelper {
    /**
     * The authorization server MUST support the use of the HTTP "GET"
     * method [RFC2616] for the authorization endpoint and MAY support the
     * use of the "POST" method as well.
     */
    private val authorizationEndpointMatcher = OrServerWebExchangeMatcher(
        PathPatternParserServerWebExchangeMatcher(OAuth2AuthorizationServerEndpointConfiguration.authorizePath(), HttpMethod.POST),
        PathPatternParserServerWebExchangeMatcher(OAuth2AuthorizationServerEndpointConfiguration.authorizePath(), HttpMethod.GET)
    )
    private val tokenEndpointMatcher = PathPatternParserServerWebExchangeMatcher(OAuth2AuthorizationServerEndpointConfiguration.tokenPath(), HttpMethod.POST)
    private val tokenRevocationEndpointMatcher =
        PathPatternParserServerWebExchangeMatcher(OAuth2AuthorizationServerEndpointConfiguration.revocationPath(), HttpMethod.POST)

    val requestMatchers = OrServerWebExchangeMatcher(
        authorizationEndpointMatcher, tokenEndpointMatcher, tokenRevocationEndpointMatcher
    )
}

