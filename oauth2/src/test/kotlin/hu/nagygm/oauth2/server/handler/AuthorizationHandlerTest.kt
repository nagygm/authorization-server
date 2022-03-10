package hu.nagygm.oauth2.server.handler

import hu.nagygm.helper.fetchBodyAsJsonNode
import hu.nagygm.oauth2.client.registration.ClientRegistrationRepository
import hu.nagygm.oauth2.config.annotation.OAuth2AuthorizationServerEndpointConfiguration.*
import hu.nagygm.oauth2.server.GrantRequestService
import hu.nagygm.oauth2.server.security.pkce.CodeChallengeMethods
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.reactive.function.server.MockServerRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.oauth2.core.OAuth2ErrorCodes.INVALID_REQUEST
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames
import org.springframework.web.reactive.function.server.HandlerStrategies
import org.springframework.web.reactive.function.server.ServerRequest

class AuthorizationHandlerTest : AnnotationSpec(){
    private var clientRegistrationRepository: ClientRegistrationRepository = mockk()
    private var grantRequestService: GrantRequestService = mockk()
    private lateinit var authorizationHandler: AuthorizationHandler

    @BeforeAll
    fun setup() {
        authorizationHandler= AuthorizationHandler(clientRegistrationRepository, grantRequestService)
    }

    @Before
    fun initTest() {
        clearMocks(grantRequestService, clientRegistrationRepository)
    }

    @Test
    fun `Calling authorize with empty request MUST return with BAD request`() {
        runBlocking {
            val serverRequest = MockServerRequest.builder().build()
            val serverResponse = authorizationHandler.authorize(serverRequest)
            serverResponse.statusCode() shouldBe HttpStatus.BAD_REQUEST
        }
    }

    @Test
    fun `Calling authorize with duplicated fields MUST return with BAD REQUEST OAUTHv2_1 section 3_1`() {
        runBlocking {

            val serverRequest = MockServerHttpRequest.get(OAuth2Api.authorize)
                .queryParam(OAuth2ParameterNames.CLIENT_ID, "1")
                .queryParam(OAuth2ParameterNames.CLIENT_ID, "2")
                .queryParam(OAuth2ParameterNames.STATE, "a_state")
                .queryParam(OAuth2ParameterNames.RESPONSE_TYPE, OAuth2AuthorizationResponseType.CODE.value)
                .queryParam(OAuth2ParameterNames.REDIRECT_URI, "a_state")
                .queryParam(PkceParameterNames.CODE_CHALLENGE, "challenge")
                .queryParam(PkceParameterNames.CODE_CHALLENGE_METHOD, CodeChallengeMethods.PLAIN.value)
                .queryParam(OAuth2ParameterNames.SCOPE, "scope")
                .build()

            val serverWebExchange = MockServerWebExchange.from(serverRequest)

            val serverResponse = authorizationHandler.authorize(ServerRequest.create(serverWebExchange, HandlerStrategies.withDefaults().messageReaders()))
            val body = serverResponse.fetchBodyAsJsonNode(serverWebExchange)
            val error = body.get("error")
            error shouldBe INVALID_REQUEST
        }
    }

}
