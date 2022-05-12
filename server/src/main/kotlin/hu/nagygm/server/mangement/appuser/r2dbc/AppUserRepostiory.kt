package hu.nagygm.server.mangement.appuser.r2dbc


import hu.nagygm.oauth2.server.client.AppUserDao
import hu.nagygm.oauth2.server.client.AppUserProfileProjection
import hu.nagygm.oauth2.server.client.AppUserProjection
import io.r2dbc.spi.Row
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*


@Repository
class R2dbcAppUserDao(@Autowired private val client: DatabaseClient) : AppUserDao {
    val rowToAppUserProfileProjection = { row: Row ->
        AppUserProfileProjectionImpl(
            userId = row.get("user_id", UUID::class.java)!!,
            username = row.get("username", String::class.java)!!,
            firstName = row.get("first_name", String::class.java)!!,
            lastName = row.get("last_name", String::class.java)!!,
            email = row.get("email", String::class.java)!!,
            enabled = row.get("enabled", java.lang.Boolean::class.java)!!.booleanValue(),
        )
    }

    val rowToAppUserProjection = { row: Row ->
        AppUserProjectionImpl(
            username = row.get("username", String::class.java)!!,
            passwordHash = row.get("passwordhash", String::class.java)!!,
            enabled = row.get("enabled", java.lang.Boolean::class.java)!!.booleanValue(),
            accountNonExpired = row.get("account_non_expired", java.lang.Boolean::class.java)!!.booleanValue(),
            credentialsNonExpired = row.get("credentials_non_expired", java.lang.Boolean::class.java)!!.booleanValue(),
            accountNonLocked = row.get("account_non_locked", java.lang.Boolean::class.java)!!.booleanValue(),
            mfa = row.get("mfa", java.lang.Boolean::class.java)!!.booleanValue()
        )
    }

    override fun fetchAllUsersWithProfile(): Flux<AppUserProfileProjectionImpl> {
        return client.sql("SELECT * FROM appuser au join appuser_profile aup on au.user_id = aup.user_id ")
            .map(rowToAppUserProfileProjection).all()
    }

    override fun fetchOneUsersWithProfile(userId: UUID): Mono<AppUserProfileProjectionImpl> {
        return client.sql("SELECT * FROM appuser au join appuser_profile aup on au.user_id = aup.user_id and au.user_id = :userId ")
            .bind("userId", userId)
            .map(rowToAppUserProfileProjection).one()
    }

    override fun findByUsername(username: String?): Mono<out AppUserProjectionImpl> {
        requireNotNull(username)
        return client.sql("SELECT * FROM appuser au where au.username = :username")
            .bind("username", username)
            .map(rowToAppUserProjection).one()
    }

    override fun getIdByUsername(username: String?): Mono<out UUID> {
        requireNotNull(username)
        return client.sql("SELECT au.user_id FROM appuser au where au.username = :username")
            .bind("username", username)
            .map { row -> row.get("user_id", UUID::class.java)!! }.one()
    }
}

data class AppUserProfileProjectionImpl (
    override val userId: UUID,
    override val username: String,
    override val firstName: String,
    override val lastName: String,
    override val email: String,
    override val enabled: Boolean,
) : AppUserProfileProjection

data class AppUserProjectionImpl (
    override val username: String,
    override val passwordHash: String,
    override val enabled: Boolean,
    override val accountNonExpired: Boolean,
    override val credentialsNonExpired: Boolean,
    override val accountNonLocked: Boolean,
    override val mfa: Boolean
) : AppUserProjection
