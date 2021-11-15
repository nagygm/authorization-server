package hu.nagygm.server.web

import hu.nagygm.server.consent.ConsentService
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
        path = ["/consent"],
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

    class ConsentFormRequest(
        var consentAccepted: Boolean,
        var id: String,
        var acceptedScopes: Set<String>,
    )
}
