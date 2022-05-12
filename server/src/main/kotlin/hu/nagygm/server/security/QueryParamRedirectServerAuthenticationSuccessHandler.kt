package hu.nagygm.server.security

import org.springframework.security.core.Authentication
import org.springframework.security.web.server.DefaultServerRedirectStrategy
import org.springframework.security.web.server.ServerRedirectStrategy
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import org.springframework.security.web.server.savedrequest.ServerRequestCache
import org.springframework.security.web.server.savedrequest.WebSessionServerRequestCache
import org.springframework.util.Assert
import reactor.core.publisher.Mono
import java.net.URI

class QueryParamRedirectServerAuthenticationSuccessHandler() : ServerAuthenticationSuccessHandler {
    private var location = URI.create("/")
    private var redirectStrategy: ServerRedirectStrategy = DefaultServerRedirectStrategy()
    private var requestCache: ServerRequestCache = WebSessionServerRequestCache()

    constructor(location: String?) : this() {
        this.location = URI.create(location)
    }

    fun setRequestCache(requestCache: ServerRequestCache) {
        Assert.notNull(requestCache, "requestCache cannot be null")
        this.requestCache = requestCache
    }

    override fun onAuthenticationSuccess(webFilterExchange: WebFilterExchange, authentication: Authentication): Mono<Void> {
        val exchange = webFilterExchange.exchange
        val redirectLocationQueryParam = exchange.request.queryParams["redirect_to"]?.get(0) ?: ""

        return requestCache.getRedirectUri(exchange).defaultIfEmpty(location)
            .flatMap { location: URI? ->
                redirectStrategy.sendRedirect(
                    exchange,
                    location
                )
            }
    }

    fun setLocation(location: URI) {
        Assert.notNull(location, "location cannot be null")
        this.location = location
    }

    fun setRedirectStrategy(redirectStrategy: ServerRedirectStrategy) {
        Assert.notNull(redirectStrategy, "redirectStrategy cannot be null")
        this.redirectStrategy = redirectStrategy
    }
}
