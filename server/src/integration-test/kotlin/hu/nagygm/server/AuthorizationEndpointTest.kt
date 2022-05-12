package hu.nagygm.server

import hu.nagygm.oauth2.config.OAuth2Api
import hu.nagygm.oauth2.config.OAuth2AuthorizationServerEndpointConfiguration
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.reactive.server.WebTestClient
import java.net.URI

@AutoConfigureWebTestClient
@SpringBootTest
class AuthorizationEndpointTest: AnnotationSpec() {
    override fun extensions() = listOf(SpringExtension)
    @Autowired
    lateinit var api: OAuth2Api


    @Autowired
    lateinit var client: WebTestClient

    @BeforeAll
    fun setup() {
    }

    @Test
    fun `When unauthenticated consent page shoud return 302 found and redirect to login`() {
        client.get().uri(api.consentPath()).exchange()
            .expectStatus().isFound.expectHeader()
            .location("/login")
    }

}