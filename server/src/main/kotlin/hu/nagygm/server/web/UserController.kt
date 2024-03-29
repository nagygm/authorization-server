package hu.nagygm.server.web

import hu.nagygm.oauth2.server.service.ConsentService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam


@Controller
class UserController(
    @Autowired val consentService: ConsentService
) {
    @GetMapping("/oauth2/v1/consent")
    suspend fun getConsentPage(
        @RequestParam("grant_request_id") grantRequestId: String,
        @RequestParam("client_id") clientId: String,
        model: Model
    ): String {
        model.addAttribute("consent", consentService.createConsent(grantRequestId, clientId))
        return "consent"
    }

//    @GetMapping("/login")
//    suspend fun loginPage(swe: ServerWebExchange): String {
////        swe.session.map { it.attributes.put("SPRING_SECURITY_SAVED_REQUEST", swe.request.headers["Referer"]) }
//        return "login2"
//    }

}
