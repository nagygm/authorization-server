package hu.nagygm.server.config

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import io.r2dbc.spi.ConnectionFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mapping.context.MappingContext
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.data.relational.core.mapping.RelationalMappingContext
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableTransactionManagement
@EnableR2dbcRepositories
class R2dbcRepositoryConfiguration : AbstractR2dbcConfiguration() {
    @Value("\${spring.postgres.url}")
    private var url: String? = null
    @Value("\${spring.postgres.host}")
    private var host: String? = null
    @Value("\${spring.postgres.port}")
    private var port: Int = 5432
    @Value("\${spring.postgres.database}")
    private var database: String? = null
    @Value("\${spring.postgres.username}")
    private var username: String? = null
    @Value("\${spring.postgres.password}")
    private var password: String? = null
    @Value("\${spring.postgres.schema}")
    private var schema: String? = null

    @Bean
    override fun connectionFactory(): ConnectionFactory {
        return PostgresqlConnectionFactory(
            PostgresqlConnectionConfiguration.builder()
                .host(host)
                .database(database)
                .username(username)
                .password(password)
                .schema(schema)
                .port(port)
                .build()
        )
    }

    @Bean
    fun transactionManager(connectionFactory: ConnectionFactory?): ReactiveTransactionManager {
        return R2dbcTransactionManager(connectionFactory)
    }

    @Bean
    fun mappingContext(): MappingContext<*, *> {
        val relationalMappingContext = RelationalMappingContext()
        relationalMappingContext.afterPropertiesSet()
        return relationalMappingContext
    }
}