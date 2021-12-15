package hu.nagygm.server.mangement.client

import io.r2dbc.spi.Row
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*


@Repository
class R2dbcClientDao(@Autowired private val client: DatabaseClient) {

    fun fetchAll(): Flux<ClientProjection> {
        return client.sql("SELECT * FROM client_registration cr")
            .map (ClientProjectionFactory::fromRow).all()
    }

    fun fetchOne(id: UUID): Mono<ClientProjection> {
        return client.sql("SELECT * FROM client_registration cr WHERE cr.client_registration_id = :id")
                .bind("id", id)
                .map (ClientProjectionFactory::fromRow).one()
    }

    fun save(clientProjection: ClientProjection): Mono<ClientProjection> {
        return client.sql("""INSERT INTO client_registration 
            (client_id, secret, redirect_uris, authorization_grant_types, scopes, access_token_lifetime, refresh_token_lifetime) 
            VALUES (:clientId, :secret, :redirectUris, :authorizationGrantTypes, :scopes, :accessTokenLifetime, :refreshTokenLifetime)
            ON CONFLICT (client_registration_id) 
            DO UPDATE SET client_id = :clientId, secret = :secret, redirect_uris = :redirectUris, authorization_grant_types = :authorizationGrantTypes, scopes = :scopes, access_token_lifetime = :accessTokenLifetime, refresh_token_lifetime = :refreshTokenLifetime
            RETURNING *
            """)
//            .bind("id", clientProjection.id)
                .bind("clientId", clientProjection.clientId)
                .bind("secret", clientProjection.secret)
                .bind("redirectUris", clientProjection.redirectUris)
                .bind("authorizationGrantTypes", clientProjection.authorizationGrantTypes)
                .bind("scopes", clientProjection.scopes)
                .bind("accessTokenLifetime", clientProjection.accessTokenLifetime)
                .bind("refreshTokenLifetime", clientProjection.refreshTokenLifetime)
                .fetch().one()
                .map (ClientProjectionFactory::fromMap)
    }

}


object ClientProjectionFactory {
    fun fromRow(row: Row): ClientProjection{
        return ClientProjection(
            id = row.get("client_registration_id", UUID::class.java),
            clientId = row.get("client_id", String::class.java),
            secret = row.get("secret", String::class.java),
            redirectUris = row.get("redirect_uris", Array<String>::class.java),
            authorizationGrantTypes = row.get("authorization_grant_types", Array<String>::class.java),
            scopes = row.get("scopes", Array<String>::class.java),
            accessTokenLifetime = row.get("access_token_lifetime", java.lang.Integer::class.java).toInt(),
            refreshTokenLifetime = row.get("refresh_token_lifetime", java.lang.Integer::class.java).toInt(),
        )
    }

    fun fromMap(row: MutableMap<String,Any>): ClientProjection{
        return ClientProjection(
            id = row["client_registration_id"] as UUID,
            clientId = row["client_id"] as String,
            secret = row["secret"] as String,
            redirectUris = row["redirect_uris"] as Array<String>,
            authorizationGrantTypes = row["authorization_grant_types"] as Array<String>,
            scopes = row["scopes"] as Array<String>,
            accessTokenLifetime = row["access_token_lifetime"] as Int,
            refreshTokenLifetime = row["refresh_token_lifetime"] as Int,
        )
    }
}

data class ClientProjection(
    var id: UUID?,
    var clientId: String?,
    var secret: String?,
    var redirectUris: Array<String>?,
    var authorizationGrantTypes: Array<String>?,
    var scopes: Array<String>?,
    var accessTokenLifetime: Int?,
    var refreshTokenLifetime: Int?
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