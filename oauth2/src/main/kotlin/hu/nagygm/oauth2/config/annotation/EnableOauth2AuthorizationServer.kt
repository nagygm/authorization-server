package hu.nagygm.oauth2.config.annotation

import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(value = [OAuth2AuthorizationServerEndpointConfiguration::class])
annotation class EnableOauth2AuthorizationServer
