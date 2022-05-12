package hu.nagygm.server.mangement.client.mongo

import hu.nagygm.oauth2.server.client.registration.ClientConfigurationParams
import hu.nagygm.oauth2.server.client.registration.ClientRegistration
import hu.nagygm.oauth2.server.client.registration.ClientRegistrationRepository
import hu.nagygm.oauth2.server.client.registration.TokenEndpointAuthMethod
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import reactor.core.publisher.Mono

interface ClientRegistrationMongoRepository : ReactiveMongoRepository<ClientRegistrationEntity, String> {
    fun findByClientId(clientId: String): Mono<ClientRegistrationEntity>
    fun findByClientIdAndSecret(clientId: String, secret: String): Mono<ClientRegistrationEntity>
}

class ClientRegistrationRepositoryImpl(@Autowired val clientRegistrationRepository: ClientRegistrationMongoRepository) :
    ClientRegistrationRepository {
    override suspend fun findById(id: String): ClientRegistration? {
        return clientRegistrationRepository.findById(id).map {
            ClientRegistration(
                it.id,
                it.clientId,
                it.secret,
                it.redirectUris,
                it.authorizationGrantTypes,
                it.scopes,
                it.clientConfiguration,
                it.tokenEndpointAuthMethod
            )
        }.awaitFirst()
    }

    override suspend fun findByClientId(clientId: String): ClientRegistration? {
        return clientRegistrationRepository.findByClientId(clientId).map {
            ClientRegistration(
                it.id,
                it.clientId,
                it.secret,
                it.redirectUris,
                it.authorizationGrantTypes,
                it.scopes,
                it.clientConfiguration,
                it.tokenEndpointAuthMethod
            )
        }.awaitFirstOrNull()
    }

    override suspend fun findByClientIdAndSecret(clientId: String, secret: String): ClientRegistration? {
        return clientRegistrationRepository.findByClientIdAndSecret(clientId, secret).map {
            ClientRegistration(
                it.id,
                it.clientId,
                it.secret,
                it.redirectUris,
                it.authorizationGrantTypes,
                it.scopes,
                it.clientConfiguration,
                it.tokenEndpointAuthMethod
            )
        }.awaitFirstOrNull()
    }

    override suspend fun save(clientRegistration: ClientRegistration): ClientRegistration {
        return clientRegistrationRepository.insert(
            ClientRegistrationEntity(
                clientRegistration.id,
                clientRegistration.clientId,
                clientRegistration.secret,
                clientRegistration.redirectUris,
                clientRegistration.authorizationGrantTypes,
                clientRegistration.scopes,
                clientRegistration.clientConfiguration,
                clientRegistration.tokenEndpointAuthMethod
            )
        ).map {
            ClientRegistration(
                it.id,
                it.clientId,
                it.secret,
                it.redirectUris,
                it.authorizationGrantTypes,
                it.scopes,
                it.clientConfiguration,
                it.tokenEndpointAuthMethod
            )
        }.awaitFirst()
    }
}

@Document
data class ClientRegistrationEntity(
    @Id
    var id: String,
    val clientId: String,
    val secret: String,
    val redirectUris: Set<String>,
    val authorizationGrantTypes: Set<AuthorizationGrantType>,
    val scopes: Set<String>,
    val clientConfiguration: Map<ClientConfigurationParams, Any>,
    val tokenEndpointAuthMethod: TokenEndpointAuthMethod
)
