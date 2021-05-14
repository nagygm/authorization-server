package hu.nagygm.oauth2.config.annotation

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import hu.nagygm.oauth2.core.Endpoint
import hu.nagygm.oauth2.server.web.AuthorizationHandler
import hu.nagygm.oauth2.server.web.TokenHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.coRouter


/**
 * Use [org.springframework.web.server.WebFilter] instead
 */
@Configuration
@ComponentScan(basePackages = ["hu.nagygm.oauth2.**"])
open class OAuth2AuthorizationServerEndpointConfiguration {

    @Bean
    open fun coRoute(@Autowired authorizationHandler: AuthorizationHandler, tokenHandler: TokenHandler) = coRouter() {
        accept(MediaType.APPLICATION_JSON).nest {
//            GET(Endpoint.TOKEN.path).invoke(tokenHandler::acquireToken)
            POST(Endpoint.TOKEN.path).invoke(tokenHandler::acquireToken)
            GET(Endpoint.AUTHORIZATION.path).invoke(authorizationHandler::authorize)
            POST(Endpoint.AUTHORIZATION.path).invoke(authorizationHandler::authorize)
        }
    }

    @Bean("tokenJsonMapper")
    open fun tokenJsonMapper(): ObjectMapper {
        return ObjectMapper().registerModule(ParameterNamesModule())
            .registerModule(Jdk8Module())
            .registerModule(JavaTimeModule())
    }
}
