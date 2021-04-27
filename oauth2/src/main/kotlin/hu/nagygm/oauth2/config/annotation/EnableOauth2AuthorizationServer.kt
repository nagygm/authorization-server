package hu.nagygm.oauth2.config.annotation

import org.springframework.context.annotation.Import

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@Import(value = [OAuth2AuthorizationServerEnpointConfiguration::class])
annotation class EnableOauth2AuthorizationServer
