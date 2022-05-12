package hu.nagygm.server.config

import hu.nagygm.server.consent.mongo.GrantRequestRepository
import hu.nagygm.server.mangement.appuser.mongo.MongoAppUserRepository
import hu.nagygm.server.mangement.client.mongo.ClientRegistrationMongoRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.mongo.MongoProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.ReactiveMongoClientFactoryBean
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories


@EnableReactiveMongoRepositories(basePackageClasses = [MongoAppUserRepository::class, GrantRequestRepository::class, ClientRegistrationMongoRepository::class])
@Configuration
class MongoConfiguration(@Autowired private val mongoProperties: MongoProperties) {

    @Bean
    fun mongo(): ReactiveMongoClientFactoryBean? {
        val clientFactory = ReactiveMongoClientFactoryBean()
        clientFactory.setConnectionString(mongoProperties.uri)
        return clientFactory
    }
}
