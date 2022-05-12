package hu.nagygm.oauth2.server.handler

import hu.nagygm.oauth2.server.service.ConsentService
import io.swagger.v3.oas.annotations.media.Schema
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseStatus
import java.net.URI

/**
 * Example of Controller based consent class, currently not Autowired
 */
class DefaultConsentController(
    @Autowired val consentService: ConsentService
)
{
    @RequestMapping(
        method = [RequestMethod.POST],
        path = ["\${oauth2.api.v1.base:/oauth2/v1}\${oauth2.api.v1.consent:/consent}"],
    )
    @ResponseStatus(HttpStatus.FOUND)
    suspend fun postConsent(form: ConsentFormRequestImpl): ResponseEntity<Void> {
        return ResponseEntity.status(HttpStatus.FOUND).header(
            "Location",
            mono {
                consentService.processConsent(
                    form.id,
                    form.consentAccepted,
                    form.acceptedScopes
                ).redirectUri
            }.map { URI.create(it).toString() }.awaitFirstOrNull()
        ).build()
    }
}

@Schema(
    description = "Consent form request",
)
data class ConsentFormRequestImpl (
    @Schema(description = "If the consent was accepted or not.", required = true)
    override val consentAccepted: Boolean,
    @Schema(description = "The ID of the consent request.", required = true)
    override val id: String,
    @Schema(description = "A partial or full set of accepted scopes from the access token request", required = true)
    override val acceptedScopes: Set<String>,
) : ConsentFormRequest

interface ConsentFormRequest {
    val consentAccepted: Boolean
    val id: String
    val acceptedScopes: Set<String>
}



