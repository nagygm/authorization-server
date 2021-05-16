package hu.nagygm.oauth2.config.default

import hu.nagygm.oauth2.core.Endpoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher

/**
 * Default security configuration for the authorization server endpoints
 */
//@Configuration
open class DefaultOAuth2ServerSecurityConfiguration {

    @Bean
    open fun serverSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http.authorizeExchange().matchers(RequestMatcherHelper.requestMatchers).authenticated().and().build()
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
        PathPatternParserServerWebExchangeMatcher(Endpoint.AUTHORIZATION.path, HttpMethod.POST),
        PathPatternParserServerWebExchangeMatcher(Endpoint.AUTHORIZATION.path, HttpMethod.GET)
    )
    private val tokenEndpointMatcher = PathPatternParserServerWebExchangeMatcher(Endpoint.TOKEN.path, HttpMethod.POST)
    private val tokenRevocationEndpointMatcher =
        PathPatternParserServerWebExchangeMatcher(Endpoint.REVOCATION.path, HttpMethod.POST)

    val requestMatchers = OrServerWebExchangeMatcher(
        authorizationEndpointMatcher, tokenEndpointMatcher, tokenRevocationEndpointMatcher
    )
}

