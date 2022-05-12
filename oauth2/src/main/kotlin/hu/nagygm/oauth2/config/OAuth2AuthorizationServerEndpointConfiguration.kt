package hu.nagygm.oauth2.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import hu.nagygm.oauth2.server.handler.AuthorizationHandler
import hu.nagygm.oauth2.server.handler.ConsentHandler
import hu.nagygm.oauth2.server.handler.TokenHandler
import kotlinx.coroutines.reactor.mono
import org.reactivestreams.Publisher
import org.springdoc.core.GroupedOpenApi
import org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder
import org.springdoc.core.fn.builders.content.Builder
import org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder
import org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.core.OAuth2AuthorizationException
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.coRouter
import reactor.core.publisher.Mono


@Configuration
@ComponentScan(basePackages = ["hu.nagygm.oauth2.**"])
class OAuth2AuthorizationServerEndpointConfiguration {

    @Autowired
    lateinit var oAuth2Api: OAuth2Api

    @Bean("tokenJsonMapper")
    fun tokenJsonMapper(): ObjectMapper {
        return ObjectMapper().registerModule(ParameterNamesModule())
            .registerModule(Jdk8Module())
            .registerModule(JavaTimeModule())
    }

    @Bean
    fun authorizeRouter(authorizationHandler: AuthorizationHandler) = coRouter {
        this.add(route().GET(oAuth2Api.authorizePath(), { r ->
            mono { authorizationHandler.authorize(r) }
                .onErrorResume(this@OAuth2AuthorizationServerEndpointConfiguration::genericErrorHandling)
        }) { ops ->
            ops
                .operationId("auhorizationGet")
                .parameter(parameterBuilder().name("response_type").description("REQUIRED, if missing will return error"))
                .parameter(parameterBuilder().name("client_id").description("REQUIRED, client identifier"))
        }.POST(oAuth2Api.authorizePath(), { r ->
            mono { authorizationHandler.authorize(r) }
                .onErrorResume(this@OAuth2AuthorizationServerEndpointConfiguration::genericErrorHandling)
        }) { ops ->
            ops
                .operationId("auhorizationPost")
                .parameter(
                    parameterBuilder().name("response_type")
                        .required(true)
                        .implementation(String::class.java)
                        .description("REQUIRED, if missing will return error"))
                .parameter(
                    parameterBuilder().name("client_id")
                        .description("REQUIRED, client identifier")
                        .implementation(String::class.java)
                )
        }.build())
    }

    @Bean
    fun tokenRouter(tokenHandler: TokenHandler) = coRouter {
        this.add(route().POST(oAuth2Api.tokenPath(), { r ->
            mono { tokenHandler.acquireToken(r) }
                .onErrorResume(this@OAuth2AuthorizationServerEndpointConfiguration::genericErrorHandling)
        }) { ops ->
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
                    responseBuilder().responseCode("302").description("Redirect response to the clients redirect URL, might return error.")
                        .content(Builder.contentBuilder())
                )
        }.build())
    }

    @Bean
    fun consentRoute(consentHandler: ConsentHandler) = coRouter {
        this.add(route().POST(oAuth2Api.consentPath(), { r ->
            mono {consentHandler.handleConsent(r)}
                .onErrorResume(this@OAuth2AuthorizationServerEndpointConfiguration::genericErrorHandling)
        }) { ops ->
            ops.operationId("consentPost")
                .parameter(parameterBuilder().name("consentAccepted").description("REQUIRED, If the consent was accepted or not."))
                .parameter(parameterBuilder().name("id").description("REQUIRED, The ID of the consent request."))
                .parameter(parameterBuilder().name("acceptedScopes").description("REQUIRED, A partial or full set of accepted scopes from the access token request"))
        }.build())
    }

    @Bean
    fun authorizationApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("oauth2-authorization-public")
            .pathsToMatch("${oAuth2Api.oauth2}/**")
            .build()
    }

    fun genericErrorHandling(e: Throwable): Mono<ServerResponse> {
        return when (e.javaClass) {
            OAuth2AuthorizationException::class.java -> ServerResponse.badRequest().body(
                Publisher {
                    val oauth2Error = (e as OAuth2AuthorizationException).error
                    OAuth2ErrorResponse(oauth2Error.errorCode, oauth2Error.description, oauth2Error.uri, "")
                },
                OAuth2ErrorResponse::class.java
            )
            else -> {
                ServerResponse.status(500).body(Mono.just(e.message ?: "Internal error"), String::class.java)
            }
        }
    }

    data class OAuth2ErrorResponse(val error: String, val errorDescription: String, val errorUri: String, val state: String)

    interface OAuth2ErrorResponseStrategy {
        fun respond(request: ServerRequest, exception: Throwable) : Mono<ServerResponse>
    }

    class RedirectOAuth2ErrorResponseStrategy : OAuth2ErrorResponseStrategy {
        override fun respond(request: ServerRequest, exception: Throwable): Mono<ServerResponse> {
            TODO("Not yet implemented")
        }
    }

    class RespondOAuth2ErrorResponseStrategy : OAuth2ErrorResponseStrategy {
        override fun respond(request: ServerRequest, exception: Throwable): Mono<ServerResponse> {
            TODO("Not yet implemented")
        }

    }

}
