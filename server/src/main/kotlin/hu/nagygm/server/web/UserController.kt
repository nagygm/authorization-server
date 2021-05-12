package hu.nagygm.server.web

import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.thymeleaf.spring5.context.webflux.IReactiveDataDriverContextVariable
import org.thymeleaf.spring5.context.webflux.ReactiveDataDriverContextVariable


@Controller
class UserController {

    @GetMapping("/consent")
    suspend fun getConsentPage(): String {
//        val reactiveDataDrivenMode: IReactiveDataDriverContextVariable = ReactiveDataDriverContextVariable(
//            movieRepository.findAll(), 1)

//        model.addAttribute("movies", reactiveDataDrivenMode)

        return "consent"
    }

    @PostMapping(
        path = ["/consent"],
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            MediaType.APPLICATION_JSON_VALUE]
    )
    suspend fun postConsent(@AuthenticationPrincipal userDetails: UserDetails): String {

        return "redirect"
    }

    data class ConsentFormRequest(
        val scopes: List<String>,
        val permissionGranted: Boolean,
        val consentId: String
    )

}
