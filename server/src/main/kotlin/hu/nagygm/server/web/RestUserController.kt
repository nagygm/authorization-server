package hu.nagygm.server.web

import hu.nagygm.oauth2.config.annotation.OAuth2AuthorizationServerEndpointConfiguration.*
import hu.nagygm.server.consent.ConsentService
import hu.nagygm.server.mangement.appuser.R2dbcAppUserDao
import io.swagger.v3.oas.annotations.media.Schema
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

@RestController
class RestUserController(
    @Autowired val consentService: ConsentService,
    @Autowired val r2dbcAppUserDao: R2dbcAppUserDao
) {
    @RequestMapping(
        method = [RequestMethod.POST],
        path = [basePathV1.oauth2 + "/consent"],
    )
    @ResponseStatus(HttpStatus.FOUND)
    suspend fun postConsent(form: ConsentFormRequest): ResponseEntity<Void> {
        return ResponseEntity.status(HttpStatus.FOUND).header(
            "Location",
            mono {
                consentService.processConsent(
                    form.id,
                    form.consentAccepted,
                    form.acceptedScopes
                ).redirectUri
            }.map { it -> URI.create(it).toString() }.awaitFirstOrNull()
        ).build()
    }

    @Schema(
        description = "Consent form request",
    )
    data class ConsentFormRequest(
        @Schema(description = "If the consent was accepted or not.", required = true)
        val consentAccepted: Boolean,
        @Schema(description = "The ID of the consent request.", required = true)
        val id: String,
        @Schema(description = "A partial or full set of accepted scopes from the access token request", required = true)
        val acceptedScopes: Set<String>,
    )

    @GetMapping(basePathV1.management + "/users")
    suspend fun getUsers(): Page<AppUserDto> {
        val results = r2dbcAppUserDao.fetchAllUsersWithProfile()
            .map { AppUserDto(it.userId.toString(), it.firstName, it.lastName, it.username, it.email, it.enabled) }
            .collectList().awaitFirst()
        return PageImpl(
            results,
            PageRequest.of(0,10), results.size.toLong()
        )
    }

    @GetMapping(basePathV1.management + "/users/{uuid}")
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
        @Schema(description = "The users first name", required = true)
        val lastName: String,
        @Schema(description = "The user username", required = true)
        val username: String,
        @Schema(description = "The user email", required = true)
        val email: String,
        @Schema(description = "The user is enabled or not", required = true)
        val enabled: Boolean,

    )
}
