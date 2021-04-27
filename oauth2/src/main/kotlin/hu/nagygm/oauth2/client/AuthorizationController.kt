package hu.nagygm.oauth2.client

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthorizationController(private val OAuth2AuthorizationRepository: OAuth2AuthorizationRepository) {

    @GetMapping("/authorize")
    suspend fun authorize(): ResponseEntity<Any> {
        OAuth2AuthorizationRepository.authorizeClient()
        return ResponseEntity.ok().build()
    }
}

data class IdentityToken(val id: String)
