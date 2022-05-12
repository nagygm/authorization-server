package hu.nagygm.oauth2.server.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import hu.nagygm.helper.RawPostRequestObjectMother
import hu.nagygm.oauth2.server.client.OAuth2AuthorizationRepository
import hu.nagygm.oauth2.server.client.registration.ClientRegistrationRepository
import hu.nagygm.oauth2.server.service.GrantRequestService
import hu.nagygm.oauth2.server.security.pkce.CodeChallengeMethods
import io.kotest.core.spec.style.AnnotationSpec
import io.mockk.clearMocks
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames

class TokenHandlerTest : AnnotationSpec() {
    private val clientRegistrationRepository: ClientRegistrationRepository = mockk()
    private val grantRequestService: GrantRequestService = mockk()
    private val oAuth2AuthorizationRepository: OAuth2AuthorizationRepository = mockk()
    private lateinit var tokenHandler: TokenHandler
    private val mapper: ObjectMapper = ObjectMapper()
        .registerModule(ParameterNamesModule())
        .registerModule(Jdk8Module())
        .registerModule(JavaTimeModule())
    private val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder(12)
    private var jwtSecretKey: String = "PjRgcjbADyoUpDtr46XCheAjdWWAF3GQ"

    @BeforeAll
    fun setup() {
        tokenHandler = TokenHandler(clientRegistrationRepository, grantRequestService, oAuth2AuthorizationRepository, mapper,
            passwordEncoder, jwtSecretKey)
    }

    @Before
    fun initTest() {
        clearMocks(clientRegistrationRepository, grantRequestService, oAuth2AuthorizationRepository)
    }


    @Test
    fun `Calling token endpoint with get method, should be not processed and response with method not supported`() {
        runBlocking {

        }
    }

    @Test
    fun `Calling token endpoint but missing grant type the should return with Invalid Request`() {
        runBlocking {

        }
    }

    @Test
    fun `Calling token endpoint but with multiple gran type should return with Invalid Request`() {
        runBlocking {

        }
    }

    @Test
    fun `Calling token endpoint with Authorization Code Grant with missing Code should return Invalid Request`() {
        runBlocking {

        }
    }

    @Test
    fun `Calling token endpoint with Authorization Code Grant with multiple Code should return Invalid Request`() {
        runBlocking {

        }
    }

    @Test
    fun `Calling token endpoint with unsupported grant type should return with Unsupported Grant Type`() {
        runBlocking {

        }
    }

    @Test
    fun `Calling token endpoint with multiple redirect Uri should return with invalid request`() {
        runBlocking {

        }
    }

    @Test
    fun `Calling token endpoint with valid authorization code request should return AccessToken`() {
        runBlocking {

        }
    }

    @Test
    fun `Calling token endpoint with valid client credentials token request should return access token`() {
        runBlocking {

        }
    }

    @Test
    fun `Calling token endpoint client credentials token request with multiple scope should return invalid request`() {
        runBlocking {

        }
    }

    @Test
    fun `Calling token endpoint refresh token request with valid request should return valid access token`() {
        runBlocking {

        }
    }

    @Test
    fun `Calling token endpoint refresh token request with missing refresh token should return invalid request`() {
        runBlocking {

        }
    }

    @Test
    fun `Calling token endpoint refresh token request with multiple scope should return invalid request`() {
        runBlocking {

        }
    }

    @Test
    fun `Calling token endpoint refresh token request with multiple refresh token should return invalid request`() {
        runBlocking {

        }
    }

    class AuthorizationCodeGrantRawPostRequestObjectMother : RawPostRequestObjectMother<AuthorizationCodeGrantRawPostRequestObjectMother>() {
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
        ): RawPostRequestObjectMother<AuthorizationCodeGrantRawPostRequestObjectMother> {
            return this.withPath(path)
                .addFormBodyValue(OAuth2ParameterNames.CLIENT_ID, clientId)
                .addFormBodyValue(OAuth2ParameterNames.STATE, state)
                .addFormBodyValue(OAuth2ParameterNames.RESPONSE_TYPE, responseType)
                .addFormBodyValue(OAuth2ParameterNames.REDIRECT_URI, redirectUri)
                .addFormBodyValue(PkceParameterNames.CODE_CHALLENGE, codeChallenge)
                .addFormBodyValue(PkceParameterNames.CODE_CHALLENGE_METHOD, codeChallengeMethod)
                .addFormBodyValue(OAuth2ParameterNames.SCOPE, scopes.joinToString(" "))
        }
    }

    class ClientCredentialsRawPostRequestObjectMother : RawPostRequestObjectMother<ClientCredentialsRawPostRequestObjectMother>() {
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
        ): RawPostRequestObjectMother<ClientCredentialsRawPostRequestObjectMother> {
            return this.withPath(path)
                .addFormBodyValue(OAuth2ParameterNames.CLIENT_ID, clientId)
                .addFormBodyValue(OAuth2ParameterNames.STATE, state)
                .addFormBodyValue(OAuth2ParameterNames.RESPONSE_TYPE, responseType)
                .addFormBodyValue(OAuth2ParameterNames.REDIRECT_URI, redirectUri)
                .addFormBodyValue(PkceParameterNames.CODE_CHALLENGE, codeChallenge)
                .addFormBodyValue(PkceParameterNames.CODE_CHALLENGE_METHOD, codeChallengeMethod)
                .addFormBodyValue(OAuth2ParameterNames.SCOPE, scopes.joinToString(" "))
        }
    }
}
