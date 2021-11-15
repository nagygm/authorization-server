package hu.nagygm.server.config

import org.springframework.security.web.reactive.result.view.CsrfRequestDataValueProcessor
import org.springframework.security.web.server.csrf.CsrfToken
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

//@ControllerAdvice
class SecurityControllerAdvice {
    @ModelAttribute
    fun csrfToken(exchange: ServerWebExchange): Mono<CsrfToken> {
//        val csrfToken: Mono<CsrfToken>? = exchange.getAttribute(CsrfToken::class.java.name)
        TODO("Add double submit cookie")
//        return csrfToken!!.doOnSuccess { token ->
//            exchange.attributes[CsrfRequestDataValueProcessor.DEFAULT_CSRF_ATTR_NAME] = token
//        }
    }
}
