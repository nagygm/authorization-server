package hu.nagygm.server.config


import hu.nagygm.server.web.ClientHandler
import kotlinx.coroutines.reactor.mono
import org.springdoc.core.GroupedOpenApi
import org.springdoc.core.fn.builders.parameter.Builder
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.core.OAuth2AuthorizationException
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.coRouter
import reactor.core.publisher.Mono
import java.util.*

@Configuration
@ComponentScan(basePackages = ["hu.nagygm.server.**"])
class AdminManagementEndpointConfiguration {

    @Autowired
    lateinit var adminManagementApi : AdminManagementApi

    @Bean
    fun adminApi(): GroupedOpenApi? {
        return GroupedOpenApi.builder()
            .group("management-admin")
            .pathsToMatch("${adminManagementApi.management}/**")
            .build()
    }

    @Bean
    fun adminManagementEndpoints(clientHandler: ClientHandler) = coRouter {
        this.add(route().GET(adminManagementApi.clientsPath(), { r ->
            mono { clientHandler.findOne(UUID.fromString(r.pathVariable("uuid"))) }
                .onErrorResume( this@AdminManagementEndpointConfiguration::genericErrorHandling)
        }){ops ->
            ops.operationId("clientsGetOneByUuid")
                .parameter(Builder.parameterBuilder().name("uuid").description("Returns registered client by uuid"))
        }.build())

    }

    fun genericErrorHandling(e: Throwable) : Mono<ServerResponse> {
        return when(e.javaClass) {
            OAuth2AuthorizationException::class.java -> ServerResponse.status(400).bodyValue(
                (e as OAuth2AuthorizationException).error
            )
            else -> {
                ServerResponse.status(500).body(Mono.just(e.message ?: "Internal error"), String::class.java)
            }
        }
    }
}