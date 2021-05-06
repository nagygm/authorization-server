package hu.nagygm.oauth2.config.annotation

import hu.nagygm.oauth2.client.AuthorizationHandler
import hu.nagygm.oauth2.core.Endpoint
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.coRouter


@Configuration
@ComponentScan(basePackages = ["hu.nagygm.oauth2.**"])
open class OAuth2AuthorizationServerEndpointConfiguration {

    @Bean
    open fun coRoute(@Autowired handler: AuthorizationHandler) = coRouter() {
        GET(Endpoint.TOKEN.path).invoke(handler::echoNumber)
    }
}
