package hu.nagygm.server.web


import hu.nagygm.oauth2.config.annotation.OAuth2AuthorizationServerEndpointConfiguration
import kotlinx.coroutines.reactor.mono
import org.springdoc.core.GroupedOpenApi
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route
import org.springframework.beans.factory.annotation.Value
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

    companion object ManagementApi {
        @Value("\${management.api.v1:/management/v1}")
        var management: String = "/management/v1"
        @Value("\${oauth2.api.v1.clients:/clients}")
        var clients: String = "/clients"
        @Value("\${oauth2.api.v1.resources:/resources}")
        var resources: String = "/resources"
        @Value("\${oauth2.api.v1.users:/users}")
        var users: String = "/users"

        inline fun clientsPath(absolute: Boolean = true) = if (absolute) "$management$clients" else clients
        inline fun resourcesPath(absolute: Boolean = true) = if (absolute) "$management$resources" else resources
        inline fun usersPath(absolute: Boolean = true) = if (absolute) "$management$users" else users
    }

    @Bean
    fun adminApi(): GroupedOpenApi? {
        return GroupedOpenApi.builder()
            .group("management-admin")
            .pathsToMatch("${management}/**")
            .build()
    }

    @Bean
    fun coRoute(clientHandler: ClientHandler) = coRouter() {
        this.add(route().POST(OAuth2AuthorizationServerEndpointConfiguration.tokenPath(), {
                r -> mono { clientHandler.findOne(UUID.fromString(r.pathVariable("uuid"))) }
            .onErrorResume( this@AdminManagementEndpointConfiguration::genericErrorHandling) }) { ops ->
            ops }.build())

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