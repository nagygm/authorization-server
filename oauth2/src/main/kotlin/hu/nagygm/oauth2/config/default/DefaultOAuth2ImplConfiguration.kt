package hu.nagygm.oauth2.config.default

import hu.nagygm.oauth2.server.client.DefaultOAuth2AuthorizationFactoryImpl
import hu.nagygm.oauth2.server.client.InMemoryOAuth2AuthorizationRepositoryImpl
import hu.nagygm.oauth2.server.client.OAuth2AuthorizationFactory
import hu.nagygm.oauth2.server.client.OAuth2AuthorizationRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DefaultOAuth2ImplConfiguration {

    @ConditionalOnMissingBean(OAuth2AuthorizationRepository::class)
    @Bean
    fun oAuth2AuthorizationRepository() : OAuth2AuthorizationRepository{
        return InMemoryOAuth2AuthorizationRepositoryImpl()
    }

    @ConditionalOnMissingBean(OAuth2AuthorizationFactory::class)
    @Bean
    fun oauth2AuthorizationFactory() : OAuth2AuthorizationFactory{
        return DefaultOAuth2AuthorizationFactoryImpl()
    }
}