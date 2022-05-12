package hu.nagygm.oauth2.server.handler

import hu.nagygm.helper.RawGetRequestObjectMother
import hu.nagygm.helper.fetchBodyAsJsonNode
import hu.nagygm.oauth2.server.client.registration.ClientRegistration
import hu.nagygm.oauth2.server.client.registration.ClientRegistrationRepository
import hu.nagygm.oauth2.server.client.registration.TokenEndpointAuthMethod
import hu.nagygm.oauth2.config.OAuth2Api
//import hu.nagygm.oauth2.config.OAuth2AuthorizationServerEndpointConfiguration.*
import hu.nagygm.oauth2.server.service.GrantRequest
import hu.nagygm.oauth2.server.service.GrantRequestService
import hu.nagygm.oauth2.server.service.GrantRequestStates
import hu.nagygm.oauth2.server.security.pkce.CodeChallengeMethods
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.reactive.function.server.MockServerRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2ErrorCodes.INVALID_REQUEST
import org.springframework.security.oauth2.core.OAuth2ErrorCodes.UNSUPPORTED_RESPONSE_TYPE
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.reactive.function.server.HandlerStrategies
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import java.time.Instant

@ContextConfiguration(classes = [(OAuth2Api::class)])
class AuthorizationHandlerTest : AnnotationSpec() {
    override fun extensions() = listOf(SpringExtension)
    private var clientRegistrationRepository: ClientRegistrationRepository = mockk()
    private var grantRequestService: GrantRequestService = mockk()
    private lateinit var authorizationHandler: AuthorizationHandler
    @Autowired
    lateinit var api: OAuth2Api

    @BeforeAll
    fun setup() {
        authorizationHandler = AuthorizationHandler(clientRegistrationRepository, grantRequestService)
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
    fun `Calling authorize with duplicated client id fields MUST return with BAD REQUEST OAUTHv2_1 section 3_1`() {
        runBlocking {
            val testData = AuthorizationCodeGrantRawGetRequestObjectMother.TestData
            coEvery { clientRegistrationRepository.findByClientId(testData.clientId) } returns
                    ClientRegistration(
                        testData.clientId, testData.clientId, testData.secret, setOf(testData.validRedirectUri, testData.validRedirectUri),
                        setOf(AuthorizationGrantType.AUTHORIZATION_CODE), emptySet(), emptyMap(), TokenEndpointAuthMethod.NONE
                    )

            val serverRequest = AuthorizationCodeGrantRawGetRequestObjectMother().validStarter(api.authorize)
                .addQueryParam(OAuth2ParameterNames.CLIENT_ID, "2").build()
            val serverWebExchange = MockServerWebExchange.from(serverRequest)

            val serverResponse =
                authorizationHandler.authorize(ServerRequest.create(serverWebExchange, HandlerStrategies.withDefaults().messageReaders()))
            val body = serverResponse.fetchBodyAsJsonNode(serverWebExchange)
            val error = body.get("error").asText()
            error shouldBe INVALID_REQUEST
        }
    }

    @Test
    fun `Calling authorize with missing client id MUST return with Invalid Request error`() {
        runBlocking {
            val testData = AuthorizationCodeGrantRawGetRequestObjectMother.TestData
            coEvery { clientRegistrationRepository.findByClientId(testData.clientId) } returns
                    ClientRegistration(
                        testData.clientId, testData.clientId, testData.secret, setOf(testData.validRedirectUri, testData.validRedirectUri),
                        setOf(AuthorizationGrantType.AUTHORIZATION_CODE), emptySet(), emptyMap(), TokenEndpointAuthMethod.NONE
                    )
            val serverRequest = AuthorizationCodeGrantRawGetRequestObjectMother().validStarter(api.authorize)
                .removeQueryParam(OAuth2ParameterNames.CLIENT_ID).build()

            val serverWebExchange = MockServerWebExchange.from(serverRequest)

            val serverResponse =
                authorizationHandler.authorize(ServerRequest.create(serverWebExchange, HandlerStrategies.withDefaults().messageReaders()))
            val body = serverResponse.fetchBodyAsJsonNode(serverWebExchange)
            val error = body.get("error").asText()
            error shouldBe INVALID_REQUEST
        }
    }


    @Test
    fun `Calling authorize with missing ResponseType MUST return with Invalid Request error`() {
        runBlocking {
            val testData = AuthorizationCodeGrantRawGetRequestObjectMother.TestData
            coEvery { clientRegistrationRepository.findByClientId(testData.clientId) } returns
                    ClientRegistration(
                        testData.clientId, testData.clientId, testData.secret, setOf(testData.validRedirectUri, testData.validRedirectUri),
                        setOf(AuthorizationGrantType.AUTHORIZATION_CODE), emptySet(), emptyMap(), TokenEndpointAuthMethod.NONE
                    )
            val serverRequest = AuthorizationCodeGrantRawGetRequestObjectMother().validStarter(api.authorize)
                .removeQueryParam(OAuth2ParameterNames.RESPONSE_TYPE).build()

            val serverWebExchange = MockServerWebExchange.from(serverRequest)

            val serverResponse =
                authorizationHandler.authorize(ServerRequest.create(serverWebExchange, HandlerStrategies.withDefaults().messageReaders()))
            val body = serverResponse.fetchBodyAsJsonNode(serverWebExchange)
            val error = body.get("error").asText()
            error shouldBe INVALID_REQUEST
        }
    }

    @Test
    fun `Calling authorize with multiple ResponseType MUST return with Invalid Request error`() {
        runBlocking {
            val testData = AuthorizationCodeGrantRawGetRequestObjectMother.TestData
            coEvery { clientRegistrationRepository.findByClientId(testData.clientId) } returns
                    ClientRegistration(
                        testData.clientId, testData.clientId, testData.secret, setOf(testData.validRedirectUri, testData.validRedirectUri),
                        setOf(AuthorizationGrantType.AUTHORIZATION_CODE), emptySet(), emptyMap(), TokenEndpointAuthMethod.NONE
                    )
            val serverRequest = AuthorizationCodeGrantRawGetRequestObjectMother().validStarter(api.authorize)
                .addQueryParam(OAuth2ParameterNames.RESPONSE_TYPE, OAuth2AuthorizationResponseType.TOKEN.value)
                .build()

            val serverWebExchange = MockServerWebExchange.from(serverRequest)

            val serverResponse =
                authorizationHandler.authorize(ServerRequest.create(serverWebExchange, HandlerStrategies.withDefaults().messageReaders()))
            val body = serverResponse.fetchBodyAsJsonNode(serverWebExchange)
            val error = body.get("error").asText()
            error shouldBe INVALID_REQUEST
        }
    }

    @Test
    fun `Calling authorize with unsupported ResponeType MUST return with Unsupported Response type error `() {
        runBlocking {
            val testData = AuthorizationCodeGrantRawGetRequestObjectMother.TestData
            coEvery { clientRegistrationRepository.findByClientId(testData.clientId) } returns
                    ClientRegistration(
                        testData.clientId, testData.clientId, testData.secret, setOf(testData.validRedirectUri, testData.validRedirectUri),
                        setOf(AuthorizationGrantType.AUTHORIZATION_CODE), emptySet(), emptyMap(), TokenEndpointAuthMethod.NONE
                    )
            val serverRequest = AuthorizationCodeGrantRawGetRequestObjectMother().validStarter(api.authorize)
                .replaceQueryParam(OAuth2ParameterNames.RESPONSE_TYPE, OAuth2AuthorizationResponseType.TOKEN.value)
                .build()

            val serverWebExchange = MockServerWebExchange.from(serverRequest)

            val serverResponse =
                authorizationHandler.authorize(ServerRequest.create(serverWebExchange, HandlerStrategies.withDefaults().messageReaders()))
            val location = serverResponse.headers().location
            val query = location.query
            val error = query.split("&").find { it.startsWith("error=[") }
            error shouldContain UNSUPPORTED_RESPONSE_TYPE
        }
    }

    @Test
    fun `Calling authorize with invalid ResponeType MUST return with Invalid Request error `() {
        runBlocking {
            val testData = AuthorizationCodeGrantRawGetRequestObjectMother.TestData
            coEvery { clientRegistrationRepository.findByClientId(testData.clientId) } returns
                    ClientRegistration(
                        testData.clientId, testData.clientId, testData.secret, setOf(testData.validRedirectUri, testData.validRedirectUri),
                        setOf(AuthorizationGrantType.AUTHORIZATION_CODE), emptySet(), emptyMap(), TokenEndpointAuthMethod.NONE
                    )
            val serverRequest = AuthorizationCodeGrantRawGetRequestObjectMother().validStarter(api.authorize)
                .replaceQueryParam(OAuth2ParameterNames.RESPONSE_TYPE, "not_valid_response_type")
                .build()

            val serverWebExchange = MockServerWebExchange.from(serverRequest)

            val serverResponse =
                authorizationHandler.authorize(ServerRequest.create(serverWebExchange, HandlerStrategies.withDefaults().messageReaders()))
            val body = serverResponse.fetchBodyAsJsonNode(serverWebExchange)
            val error = body.get("error").asText()
            error shouldBe INVALID_REQUEST
        }
    }

    @Test
    fun `Calling authorize with multiple RedirectUris MUST return with Invalid Request `() {
        runBlocking {
            val testData = AuthorizationCodeGrantRawGetRequestObjectMother.TestData
            coEvery { clientRegistrationRepository.findByClientId(testData.clientId) } returns
                    ClientRegistration(
                        testData.clientId, testData.clientId, testData.secret, setOf(testData.validRedirectUri, testData.validRedirectUri),
                        setOf(AuthorizationGrantType.AUTHORIZATION_CODE), emptySet(), emptyMap(), TokenEndpointAuthMethod.NONE
                    )
            val serverRequest = AuthorizationCodeGrantRawGetRequestObjectMother().validStarter(api.authorize)
                .addQueryParam(OAuth2ParameterNames.REDIRECT_URI, testData.validRedirectUri2)
                .build()

            val serverWebExchange = MockServerWebExchange.from(serverRequest)

            val serverResponse =
                authorizationHandler.authorize(ServerRequest.create(serverWebExchange, HandlerStrategies.withDefaults().messageReaders()))
            val body = serverResponse.fetchBodyAsJsonNode(serverWebExchange)
            val error = body.get("error").asText()
            error shouldBe INVALID_REQUEST
        }
    }

    @Test
    fun `Calling authorize with multiple State fields MUST return with Invalid Request `() {
        runBlocking {
            val testData = AuthorizationCodeGrantRawGetRequestObjectMother.TestData
            coEvery { clientRegistrationRepository.findByClientId(testData.clientId) } returns
                    ClientRegistration(
                        testData.clientId, testData.clientId, testData.secret, setOf(testData.validRedirectUri, testData.validRedirectUri),
                        setOf(AuthorizationGrantType.AUTHORIZATION_CODE), emptySet(), emptyMap(), TokenEndpointAuthMethod.NONE
                    )

            val serverRequest = AuthorizationCodeGrantRawGetRequestObjectMother().validStarter(api.authorize)
                .addQueryParam(OAuth2ParameterNames.STATE, "a_state2")
                .build()

            val serverWebExchange = MockServerWebExchange.from(serverRequest)

            val serverResponse =
                authorizationHandler.authorize(ServerRequest.create(serverWebExchange, HandlerStrategies.withDefaults().messageReaders()))
            val body = serverResponse.fetchBodyAsJsonNode(serverWebExchange)
            val error = body.get("error").asText()
            error shouldBe INVALID_REQUEST
        }
    }

    @Test
    fun `Calling authorize with multiple CodeChallenge MUST return with Invalid Request`() {
        runBlocking {
            val testData = AuthorizationCodeGrantRawGetRequestObjectMother.TestData
            coEvery { clientRegistrationRepository.findByClientId(testData.clientId) } returns
                    ClientRegistration(
                        testData.clientId, testData.clientId, testData.secret, setOf(testData.validRedirectUri, testData.validRedirectUri),
                        setOf(AuthorizationGrantType.AUTHORIZATION_CODE), emptySet(), emptyMap(), TokenEndpointAuthMethod.NONE
                    )

            val serverRequest = AuthorizationCodeGrantRawGetRequestObjectMother().validStarter(api.authorize)
                .addQueryParam(PkceParameterNames.CODE_CHALLENGE, "shortestvalidcodechallengebyspecification432")
                .build()

            val serverWebExchange = MockServerWebExchange.from(serverRequest)

            val serverResponse =
                authorizationHandler.authorize(ServerRequest.create(serverWebExchange, HandlerStrategies.withDefaults().messageReaders()))
            val body = serverResponse.fetchBodyAsJsonNode(serverWebExchange)
            val error = body.get("error").asText()
            error shouldBe INVALID_REQUEST
        }
    }

    @Test
    fun `Calling authorize with multiple CodeChallenge Method MUST return with Invalid Request`() {
        runBlocking {
            val testData = AuthorizationCodeGrantRawGetRequestObjectMother.TestData
            coEvery { clientRegistrationRepository.findByClientId(testData.clientId) } returns
                    ClientRegistration(
                        testData.clientId, testData.clientId, testData.secret, setOf(testData.validRedirectUri, testData.validRedirectUri),
                        setOf(AuthorizationGrantType.AUTHORIZATION_CODE), emptySet(), emptyMap(), TokenEndpointAuthMethod.NONE
                    )
            val serverRequest = AuthorizationCodeGrantRawGetRequestObjectMother().validStarter(api.authorize)
                .addQueryParam(PkceParameterNames.CODE_CHALLENGE_METHOD, CodeChallengeMethods.S256.value)
                .build()

            val serverWebExchange = MockServerWebExchange.from(serverRequest)

            val serverResponse =
                authorizationHandler.authorize(ServerRequest.create(serverWebExchange, HandlerStrategies.withDefaults().messageReaders()))
            val body = serverResponse.fetchBodyAsJsonNode(serverWebExchange)
            val error = body.get("error").asText()
            error shouldBe INVALID_REQUEST
        }
    }

    @Test
    fun `Calling authorize with valid request returning successful authorization code grant redirection to consent page`() {
        runBlocking {
            val testData = AuthorizationCodeGrantRawGetRequestObjectMother.TestData
            coEvery { clientRegistrationRepository.findByClientId(testData.clientId) } returns
                    ClientRegistration(
                        testData.clientId, testData.clientId, testData.secret, setOf(testData.validRedirectUri, testData.validRedirectUri),
                        setOf(AuthorizationGrantType.AUTHORIZATION_CODE), emptySet(), emptyMap(), TokenEndpointAuthMethod.NONE
                    )
            val grantRequest = createGrantRequest(
                testData.validRedirectUri, testData.validScopes, OAuth2AuthorizationResponseType.CODE.value, testData.clientId,
                testData.clientId, "1234567890", testData.state, Instant.now(), GrantRequestStates.ConsentRequested.code, emptySet(),
                "1", Instant.now(), Instant.now(), testData.validCodeChallenge, CodeChallengeMethods.PLAIN.value
            )
            coEvery { grantRequestService.saveGrantRequest(any()) } returns grantRequest

            val serverResponse = createValidAuthorizationFromGrantRequest(grantRequest)
            val location = serverResponse.headers().location

            serverResponse.statusCode() shouldBe HttpStatus.FOUND
            location shouldNotBe null
            location?.path shouldContain api.consent
        }
    }

    private suspend fun createValidAuthorizationFromGrantRequest(grantRequest: GrantRequest): ServerResponse {
        val serverRequest = MockServerHttpRequest.get(api.authorize)
            .queryParam(OAuth2ParameterNames.CLIENT_ID, grantRequest.clientId)
            .queryParam(OAuth2ParameterNames.STATE, grantRequest.state)
            .queryParam(OAuth2ParameterNames.RESPONSE_TYPE, grantRequest.responseType)
            .queryParam(OAuth2ParameterNames.REDIRECT_URI, grantRequest.redirectUri)
            .queryParam(PkceParameterNames.CODE_CHALLENGE, grantRequest.codeChallenge)
            .queryParam(PkceParameterNames.CODE_CHALLENGE_METHOD, grantRequest.codeChallengeMethod)
            .queryParam(OAuth2ParameterNames.SCOPE, grantRequest.scopes)
            .build()


        val serverWebExchange = MockServerWebExchange.from(serverRequest)

        return authorizationHandler.authorize(ServerRequest.create(serverWebExchange, HandlerStrategies.withDefaults().messageReaders()))
    }

    private fun createGrantRequest(
        redirectUri: String,
        scopes: Set<String>,
        responseType: String,
        clientId: String,
        id: String?,
        code: String?,
        state: String,
        codeCreatedAt: Instant?,
        requestState: String,
        acceptedScopes: Set<String>,
        associatedUserId: String?,
        consentRequestedAt: Instant?,
        processedAt: Instant?,
        codeChallenge: String?,
        codeChallengeMethod: String?
    ): GrantRequest {
        return object : GrantRequest {
            override val redirectUri: String = redirectUri
            override val scopes: Set<String> = scopes
            override val responseType: String = responseType
            override val clientId: String = clientId
            override var id: String? = id
            override var code: String? = code
            override val state: String = state
            override var codeCreatedAt: Instant? = codeCreatedAt
            override var requestState: String = requestState
            override var acceptedScopes: Set<String> = acceptedScopes
            override var associatedUserId: String? = associatedUserId
            override var processedAt: Instant? = consentRequestedAt
            override var consentRequestedAt: Instant? = processedAt
            override var codeChallenge: String? = codeChallenge
            override var codeChallengeMethod: String? = codeChallengeMethod
        }
    }

    class AuthorizationCodeGrantRawGetRequestObjectMother : RawGetRequestObjectMother<AuthorizationCodeGrantRawGetRequestObjectMother>() {
        companion object TestData {
            val validRedirectUri = "http://redirect.uri.com"
            val validRedirectUri2 = "http://redirect.uri2.com"
            val invalidRedirectUri = "not-valid-uri"
            val state = "a-state"
            val validCodeChallenge = "shortestvalidcodechallengebyspecification43"
            val clientId = "1"
            val validScopes = setOf("scope1", "scope2")
            val secret = "secret"
        }
        fun validStarter(
            path: String, clientId: String = TestData.clientId, state: String = TestData.state,
            responseType: String = OAuth2AuthorizationResponseType.CODE.value, redirectUri: String = TestData.validRedirectUri,
            codeChallenge: String? = TestData.validCodeChallenge, codeChallengeMethod: String? = CodeChallengeMethods.PLAIN.value,
            scopes: Set<String> = TestData.validScopes
        ): RawGetRequestObjectMother<AuthorizationCodeGrantRawGetRequestObjectMother> {
            return this.withPath(path)
                .addQueryParam(OAuth2ParameterNames.CLIENT_ID, clientId)
                .addQueryParam(OAuth2ParameterNames.STATE, state)
                .addQueryParam(OAuth2ParameterNames.RESPONSE_TYPE, responseType)
                .addQueryParam(OAuth2ParameterNames.REDIRECT_URI, redirectUri)
                .addQueryParam(PkceParameterNames.CODE_CHALLENGE, codeChallenge)
                .addQueryParam(PkceParameterNames.CODE_CHALLENGE_METHOD, codeChallengeMethod)
                .addQueryParam(OAuth2ParameterNames.SCOPE, scopes.joinToString(" "))
        }
    }
}

