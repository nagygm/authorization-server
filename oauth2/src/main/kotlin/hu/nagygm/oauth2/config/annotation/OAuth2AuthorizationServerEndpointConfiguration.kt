package hu.nagygm.oauth2.config.annotation

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import hu.nagygm.oauth2.server.handler.AuthorizationHandler
import hu.nagygm.oauth2.server.handler.TokenHandler
import kotlinx.coroutines.reactor.mono
import org.springdoc.core.GroupedOpenApi
import org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder
import org.springdoc.core.fn.builders.content.Builder
import org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.core.OAuth2AuthorizationException
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.coRouter
import reactor.core.publisher.Mono


@Configuration
@ComponentScan(basePackages = ["hu.nagygm.oauth2.**"])
class OAuth2AuthorizationServerEndpointConfiguration {

    companion object OAuth2Api {
        @Value("\${oauth2.api.v1.base:/oauth2/v1}")
        var oauth2: String = "/oauth2/v1"
        @Value("\${oauth2.api.v1.authorize:/authorize}")
        var authorize: String = "/authorize"
        @Value("\${oauth2.api.v1.token:/token}")
        var token: String = "/token"
        @Value("\${oauth2.api.v1.consent:/consent}")
        var consent: String = "/consent"
        @Value("\${oauth2.api.v1.userinfo:/userinfo}")
        var userinfo: String = "/userinfo"
        @Value("\${oauth2.api.v1.endsession:/endsession}")
        var endsession: String = "/endsession"
        @Value("\${oauth2.api.v1.checksession:/checksession}")
        var checksession: String = "/checksession"
        @Value("\${oauth2.api.v1.revocation:/revocation}")
        var revocation: String = "/revocation"
        @Value("\${oauth2.api.v1.introspect:/introspect}")
        var introspect: String = "/introspect"

        inline fun authorizePath(absolute: Boolean = true) = if (absolute) "$oauth2$authorize" else authorize
        inline fun tokenPath(absolute: Boolean = true) = if (absolute) "$oauth2$token" else token
        inline fun consentPath(absolute: Boolean = true) = if (absolute) "$oauth2$consent" else consent
        inline fun userinfoPath(absolute: Boolean = true) = if (absolute) "$oauth2$userinfo" else userinfo
        inline fun endsessionPath(absolute: Boolean = true) = if (absolute) "$oauth2$endsession" else endsession
        inline fun checksessionPath(absolute: Boolean = true) = if (absolute) "$oauth2$checksession" else checksession
        inline fun revocationPath(absolute: Boolean = true) = if (absolute) "$oauth2$revocation" else revocation
        inline fun introspectPath(absolute: Boolean = true) = if (absolute) "$oauth2$introspect" else introspect
    }




    @Bean("tokenJsonMapper")
    fun tokenJsonMapper(): ObjectMapper {
        return ObjectMapper().registerModule(ParameterNamesModule())
            .registerModule(Jdk8Module())
            .registerModule(JavaTimeModule())
    }


    @Bean
    fun coRoute(authorizationHandler: AuthorizationHandler, tokenHandler: TokenHandler) = coRouter() {
        this.add(route().POST(tokenPath(), { r -> mono { tokenHandler.acquireToken(r) }
            .onErrorResume( this@OAuth2AuthorizationServerEndpointConfiguration::genericErrorHandling) }) { ops ->
            ops
                .operationId("tokenPost")
                .parameter(parameterBuilder().name("client_id").description("REQUIRED, client authentication id"))
                .parameter(
                    parameterBuilder().name("scope")
                        .description("OPTIONAL, space delimited scope entires the access token requested for. scope = scope-token *( SP scope-token ); scope-token = 1*( %x21 / %x23-5B / %x5D-7E )")
                )
                .parameter(
                    parameterBuilder().name("scope").description(
                        """REQUIRED.  Identifier of the grant type the client uses with the particular token request.  This specification defines the values authorization_code, refresh_token, and client_credentials.  The grant type determines the further parameters required or supported by the token request.  The details of those grant types are defined below."""
                    )
                )
                .response(
                    responseBuilder().responseCode("200").description("This is normal response description")
                        .content(Builder.contentBuilder())
                )
                .response(
                    responseBuilder().responseCode("400").description("This is another response description")
                        .content(Builder.contentBuilder())
                )
        }.GET(authorizePath(), { r -> mono { authorizationHandler.authorize(r) }
            .onErrorResume( this@OAuth2AuthorizationServerEndpointConfiguration::genericErrorHandling) }) { ops ->
            ops
                .operationId("auhorizationGet")
                .parameter(parameterBuilder().name("response_type").description("REQUIRED, if missing will return error"))
                .parameter(parameterBuilder().name("client_id").description("REQUIRED, client identifier"))
        }.POST(authorizePath(), { r -> mono { authorizationHandler.authorize(r) }
            .onErrorResume( this@OAuth2AuthorizationServerEndpointConfiguration::genericErrorHandling) }) { ops ->
            ops
                .operationId("auhorizationPost")
                .parameter(parameterBuilder().name("response_type").description("REQUIRED, if missing will return error"))
                .parameter(parameterBuilder().name("client_id").description("REQUIRED, client identifier"))
        }.build())
    }

    @Bean
    fun authorizationApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("oauth2-authorization-public")
            .pathsToMatch("${oauth2}/**")
            .build()
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
