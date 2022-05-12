package hu.nagygm.server.mangement.client.r2dbc

import hu.nagygm.oauth2.server.client.registration.*
import io.r2dbc.spi.Row
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*


@Repository
class R2dbcClientDao(@Autowired private val client: DatabaseClient) {

    fun findByClientId(clientId: String): Mono<ClientProjection> {
        return client.sql("SELECT * FROM client_registration cr WHERE cr.client_id = :clientId")
            .bind("clientId", clientId)
            .map (ClientProjectionFactory::fromRow).one()
    }

    fun findByClientIdAndSecret(clientId: String, secret: String): Mono<ClientProjection>{
        return client.sql("SELECT * FROM client_registration cr WHERE cr.client_id = :clientId and cr.secret = :secret")
            .bind("clientId", clientId)
            .bind("secret", secret)
            .map (ClientProjectionFactory::fromRow).one()
    }

    fun fetchAll(): Flux<ClientProjection> {
        return client.sql("SELECT * FROM client_registration cr")
            .map (ClientProjectionFactory::fromRow).all()
    }

    fun fetchOne(id: UUID): Mono<ClientProjection> {
        return client.sql("SELECT * FROM client_registration cr WHERE cr.client_registration_id::text = :id")
                .bind("id", id.toString())
                .map (ClientProjectionFactory::fromRow).one()
    }

    fun save(clientProjection: ClientProjection): Mono<ClientProjection> {
        return client.sql("""INSERT INTO client_registration 
            (client_id, secret, redirect_uris, authorization_grant_types, scopes, access_token_lifetime, refresh_token_lifetime, token_endpoint_auth_method) 
            VALUES (:clientId, :secret, :redirectUris, :authorizationGrantTypes, :scopes, :accessTokenLifetime, :refreshTokenLifetime, :token_endpoint_auth_method)
            ON CONFLICT (client_registration_id) 
            DO UPDATE SET client_id = :clientId, secret = :secret, redirect_uris = :redirectUris, authorization_grant_types = :authorizationGrantTypes, scopes = :scopes, access_token_lifetime = :accessTokenLifetime, refresh_token_lifetime = :refreshTokenLifetime
            RETURNING *
            """
        )
//            .bind("id", clientProjection.id)
            .bind("clientId", clientProjection.clientId)
            .bind("secret", clientProjection.secret)
            .bind("redirectUris", clientProjection.redirectUris)
            .bind("authorizationGrantTypes", clientProjection.authorizationGrantTypes)
            .bind("scopes", clientProjection.scopes)
            .bind("accessTokenLifetime", clientProjection.accessTokenLifetime)
            .bind("refreshTokenLifetime", clientProjection.refreshTokenLifetime)
            .bind("token_endpoint_auth_method", clientProjection.tokenEndpointAuthMethod)
            .fetch().one()
            .map(ClientProjectionFactory::fromMap)
    }

}


object ClientProjectionFactory {
    fun fromRow(row: Row): ClientProjection {
        return ClientProjection(
            id = row.get("client_registration_id", UUID::class.java),
            clientId = row.get("client_id", String::class.java),
            secret = row.get("secret", String::class.java),
            redirectUris = row.get("redirect_uris", Array<String>::class.java),
            authorizationGrantTypes = row.get("authorization_grant_types", Array<String>::class.java),
            scopes = row.get("scopes", Array<String>::class.java),
            accessTokenLifetime = row.get("access_token_lifetime", Integer::class.java).toInt(),
            refreshTokenLifetime = row.get("refresh_token_lifetime", Integer::class.java).toInt(),
            tokenEndpointAuthMethod = row["token_endpoint_auth_method"] as String
        )
    }

    fun fromMap(row: MutableMap<String,Any>): ClientProjection {
        return ClientProjection(
            id = row["client_registration_id"] as UUID,
            clientId = row["client_id"] as String,
            secret = row["secret"] as String,
            redirectUris = row["redirect_uris"] as Array<String>,
            authorizationGrantTypes = row["authorization_grant_types"] as Array<String>,
            scopes = row["scopes"] as Array<String>,
            accessTokenLifetime = row["access_token_lifetime"] as Int,
            refreshTokenLifetime = row["refresh_token_lifetime"] as Int,
            tokenEndpointAuthMethod = row["token_endpoint_auth_method"] as String
        )
    }
}

data class ClientProjection(
    var id: UUID,
    var clientId: String,
    var secret: String,
    var redirectUris: Array<String>,
    var authorizationGrantTypes: Array<String>,
    var scopes: Array<String>,
    var accessTokenLifetime: Int,
    var refreshTokenLifetime: Int,
    val tokenEndpointAuthMethod: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClientProjection

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

@Service
class ClientRegistrationRepositoryImpl(@Autowired val r2dbcClientDao: R2dbcClientDao) :
    ClientRegistrationRepository {
    override suspend fun findById(id: String): ClientRegistration? {
        return r2dbcClientDao.fetchOne(UUID.fromString(id)).map {
            ClientRegistration(
                it.id.toString(),
                it.clientId,
                it.secret,
                it.redirectUris.toSet(),
                it.authorizationGrantTypes.map { agt -> AuthorizationGrantType(agt) }.toSet(),
                it.scopes.toSet(),
                mapOf(ClientConfigurationParams.ACCESS_TOKEN_LIFETIME to it.accessTokenLifetime, ClientConfigurationParams.REFRESH_TOKEN_LIFETIME to it.refreshTokenLifetime),
                TokenEndpointAuthMethod.valueOf(it.tokenEndpointAuthMethod)
            )
        }.awaitFirst()
    }

    override suspend fun findByClientId(clientId: String): ClientRegistration? {
        return r2dbcClientDao.findByClientId(clientId).map {
            ClientRegistration(
                it.id.toString(),
                it.clientId,
                it.secret,
                it.redirectUris.toSet(),
                it.authorizationGrantTypes.map { agt -> AuthorizationGrantType(agt) }.toSet(),
                it.scopes.toSet(),
                mapOf(ClientConfigurationParams.ACCESS_TOKEN_LIFETIME to it.accessTokenLifetime, ClientConfigurationParams.REFRESH_TOKEN_LIFETIME to it.refreshTokenLifetime),
                TokenEndpointAuthMethod.valueOf(it.tokenEndpointAuthMethod)
            )
        }.awaitFirstOrNull()
    }

    override suspend fun findByClientIdAndSecret(clientId: String, secret: String): ClientRegistration? {
        return r2dbcClientDao.findByClientIdAndSecret(clientId, secret).map {
            ClientRegistration(
                it.id.toString(),
                it.clientId,
                it.secret,
                it.redirectUris.toSet(),
                it.authorizationGrantTypes.map { agt -> AuthorizationGrantType(agt) }.toSet(),
                it.scopes.toSet(),
                mapOf(ClientConfigurationParams.ACCESS_TOKEN_LIFETIME to it.accessTokenLifetime, ClientConfigurationParams.REFRESH_TOKEN_LIFETIME to it.refreshTokenLifetime),
                TokenEndpointAuthMethod.valueOf(it.tokenEndpointAuthMethod)
            )
        }.awaitFirstOrNull()
    }

    override suspend fun save(clientRegistration: ClientRegistration): ClientRegistration {
        return r2dbcClientDao.save(
            ClientProjection(
                UUID.fromString(clientRegistration.id),
                clientRegistration.clientId,
                clientRegistration.secret,
                clientRegistration.redirectUris.toTypedArray(),
                clientRegistration.authorizationGrantTypes.map { it.value }.toTypedArray(),
                clientRegistration.scopes.toTypedArray(),
                clientRegistration.clientConfiguration.getOrDefault(ClientConfigurationParams.ACCESS_TOKEN_LIFETIME, 3600) as Int,
                clientRegistration.clientConfiguration.getOrDefault(ClientConfigurationParams.REFRESH_TOKEN_LIFETIME, 3600) as Int,
                clientRegistration.tokenEndpointAuthMethod.value
            )
        ).map {
            ClientRegistration(
                it.id.toString(),
                it.clientId,
                it.secret,
                it.redirectUris.toSet(),
                it.authorizationGrantTypes.map { agt -> AuthorizationGrantType(agt) }.toSet(),
                it.scopes.toSet(),
                mapOf(ClientConfigurationParams.ACCESS_TOKEN_LIFETIME to it.accessTokenLifetime, ClientConfigurationParams.REFRESH_TOKEN_LIFETIME to it.refreshTokenLifetime),
                TokenEndpointAuthMethod.valueOf(it.tokenEndpointAuthMethod)
            )
        }.awaitFirst()
    }
}

