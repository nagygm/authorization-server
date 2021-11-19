package hu.nagygm.server.web

import hu.nagygm.server.consent.ConsentService
import io.swagger.v3.oas.annotations.media.Schema
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
class RestUserController(
    @Autowired val consentService: ConsentService
) {
    @RequestMapping(
        method = [RequestMethod.POST],
        path = ["/oauth2/v1/consent"],
    )
    @ResponseStatus(HttpStatus.FOUND)
    suspend fun postConsent(form: ConsentFormRequest): ResponseEntity<Void> {
        return ResponseEntity.status(HttpStatus.FOUND).header(
            "Location",
            URI.create(mono {
                consentService.processConsent(
                    form.id,
                    form.consentAccepted,
                    form.acceptedScopes
                ).redirectUri
            }.awaitFirstOrNull())
                .toString()
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
}
