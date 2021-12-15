package hu.nagygm.server.mangement.appuser


import hu.nagygm.server.web.RestUserController
import hu.nagygm.server.web.RestUserController.AppUserDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*


@Repository
class R2dbcAppUserDao(@Autowired private val client: DatabaseClient) {

    fun fetchAllUsersWithProfile(): Flux<AppUserProfileProjection> {
        return client.sql("SELECT * FROM appuser au join appuser_profile aup on au.user_id = aup.user_id ")
            .map { row ->
                AppUserProfileProjection(
                    userId = row.get("user_id", UUID::class.java),
                    username = row.get("username", String::class.java),
                    firstName = row.get("first_name", String::class.java),
                    lastName = row.get("last_name", String::class.java),
                    email = row.get("email", String::class.java),
                    enabled = row.get("enabled", java.lang.Boolean::class.java).booleanValue(),
                )
            }.all()
    }

    fun fetchOneUsersWithProfile(userId: UUID): Mono<AppUserProfileProjection> {
        return client.sql("SELECT * FROM appuser au join appuser_profile aup on au.user_id = aup.user_id and au.user_id = :userId ")
            .bind("userId", userId)
            .map { row ->
                AppUserProfileProjection(
                    userId = row.get("user_id", UUID::class.java),
                    username = row.get("username", String::class.java),
                    firstName = row.get("first_name", String::class.java),
                    lastName = row.get("last_name", String::class.java),
                    email = row.get("email", String::class.java),
                    enabled = row.get("enabled", java.lang.Boolean::class.java).booleanValue(),
                )
            }.one()
    }
}

data class AppUserProfileProjection(
    val userId: UUID,
    val username: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val enabled: Boolean,
)


