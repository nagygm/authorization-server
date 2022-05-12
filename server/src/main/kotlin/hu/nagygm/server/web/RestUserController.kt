package hu.nagygm.server.web

import hu.nagygm.server.mangement.appuser.r2dbc.R2dbcAppUserDao
import io.swagger.v3.oas.annotations.media.Schema
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.*
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class RestUserController(
    @Autowired val r2dbcAppUserDao: R2dbcAppUserDao
) {
    @GetMapping("/management/v1/users")
    suspend fun getUsers(): Page<AppUserDto> {
        val results = r2dbcAppUserDao.fetchAllUsersWithProfile()
            .map { AppUserDto(it.userId.toString(), it.firstName, it.lastName, it.username, it.email, it.enabled) }
            .collectList().awaitFirst()
        return PageImpl(
            results,
            PageRequest.of(0,10), results.size.toLong()
        )
    }

    @GetMapping("/management/v1/users/{uuid}")
    suspend fun getUser(@PathVariable uuid: UUID): AppUserDto {
        val result = r2dbcAppUserDao.fetchOneUsersWithProfile(uuid)
            .map { AppUserDto(it.userId.toString(), it.firstName,it.lastName, it.username, it.email, it.enabled) }
            .awaitFirst()
        return result
    }

    @Schema(description = "Get all users")
    data class AppUserDto(
        @Schema(description = "The user ID", required = true)
        val id: String,
        @Schema(description = "The users first name", required = true)
        val firstName: String,
        @Schema(description = "The users last name", required = true)
        val lastName: String,
        @Schema(description = "The user username", required = true)
        val username: String,
        @Schema(description = "The user email", required = true)
        val email: String,
        @Schema(description = "The user is enabled or not", required = true)
        val enabled: Boolean,

    )
}
