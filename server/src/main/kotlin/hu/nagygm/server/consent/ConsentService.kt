package hu.nagygm.server.consent

import hu.nagygm.oauth2.client.registration.ClientRegistrationRepository
import hu.nagygm.oauth2.server.GrantRequestStates
import hu.nagygm.oauth2.server.GrantRequest
import hu.nagygm.oauth2.server.GrantRequestService
import hu.nagygm.oauth2.server.handler.AuthorizationHandler
import hu.nagygm.server.security.MongoAppUserRepository
import hu.nagygm.server.security.UserService
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class ConsentServiceImpl(
    @Autowired val userService: UserService,
    @Autowired val mongoAppUserRepository: MongoAppUserRepository,
    @Autowired val grantRequestService: GrantRequestService,
    @Autowired val clientRegistrationRepository: ClientRegistrationRepository
) : ConsentService {

    private val codeGenerator = Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 96)

    override suspend fun createConsent(grantRequestId: String, clientId: String): ConsentPageResponse {
        val user = userService.getCurrentUser() ?: throw AccessDeniedException("Access denied: can't find user")
        val appUserDetails = mongoAppUserRepository.findByUsername(user.username)
        val grantRequest = grantRequestService.getGrantRequestByIdAndClientId(grantRequestId, clientId)
            ?: throw AccessDeniedException("Access denied: can't find grant request")
        grantRequest.requestState = GrantRequestStates.ConsentRequested.code
        grantRequest.associatedUserId = appUserDetails.id
        grantRequest.consentRequestedAt = Instant.now()
        grantRequest.processedAt = null
        grantRequestService.save(grantRequest)
        return ConsentPageResponse(grantRequest.id, grantRequest.scopes, grantRequest.redirectUri, grantRequest.clientId)
    }

    override suspend fun processConsent(grantRequestId: String, accept: Boolean, acceptedScopes: Set<String>): ConsentProcessResponse {
        require(grantRequestId.isNotEmpty()) { "ID can't be empty" }
        val user = userService.getCurrentUser() ?: throw AccessDeniedException("Access denied: can't find user")
        //TODO refactor checks and responses to library
        val appUserDetails = mongoAppUserRepository.findByUsername(user.username)

        val grantRequest = grantRequestService.getGrantRequestById(grantRequestId, appUserDetails.id)
            ?: throw AccessDeniedException("Access denied: can't find grant request")
        if (!grantRequest.scopes.containsAll(acceptedScopes)) {
            val redirectUri = getRedirectUri(grantRequest)
            return ConsentProcessResponse("${redirectUri}?${OAuth2ParameterNames.ERROR}=${OAuth2ErrorCodes.INVALID_SCOPE}")
        }
        return if (accept) {
            grantRequest.code = codeGenerator.generateKey()
            grantRequest.codeCreatedAt = Instant.now()
            grantRequest.acceptedScopes = acceptedScopes
            grantRequest.requestState = GrantRequestStates.ConsentAccepted.code
            grantRequestService.save(grantRequest)
            ConsentProcessResponse("${grantRequest.redirectUri}?${OAuth2ParameterNames.CODE}=${grantRequest.code}&${OAuth2ParameterNames.STATE}=${grantRequest.state}")
        } else {
            grantRequest.acceptedScopes = emptySet()
            grantRequest.requestState = GrantRequestStates.ConsentRejected.code
            grantRequestService.save(grantRequest)
            ConsentProcessResponse("${grantRequest.redirectUri}?${OAuth2ParameterNames.ERROR}=${OAuth2ErrorCodes.ACCESS_DENIED}")
        }
    }

    private suspend fun getRedirectUri(grantRequest: GrantRequest) : String {
        return grantRequest.redirectUri.ifBlank {
            val redirectUri = clientRegistrationRepository.findByClientId(grantRequest.clientId)?.redirectUris?.first()
            return redirectUri ?: throw AccessDeniedException("Access denied: can't find redirect uri")
        }
    }

}

interface ConsentService {
    suspend fun createConsent(grantRequestId: String, clientId: String): ConsentPageResponse
    suspend fun processConsent(id: String, accept: Boolean, acceptedScopes: Set<String>): ConsentProcessResponse
}

data class ConsentPageResponse(
    var id: String?,
    val scopes: Set<String>,
    val redirectUrl: String,
    val clientId: String
)

data class ConsentProcessResponse(
    val redirectUri: String
)

@Service
class GrantRequestServiceImpl(@Autowired val grantRequestRepository: GrantRequestRepository) : GrantRequestService {
    override suspend fun saveGrantRequest(request: AuthorizationHandler.AuthorizationRequest): GrantRequest {
        return grantRequestRepository.save(
            GrantRequestEntity(
                request.redirectUri,
                request.scopes,
                request.responseType,
                request.clientId,
                null,
                null,
                request.state ?: "",
                null,
                GrantRequestStates.Created.code,
                emptySet(),
                null,
                null,
                null,
                request.codeChallenge,
                request.codeChallengeMethod
            )
        ).awaitFirst()
    }

    override suspend fun getGrantRequestByIdAndClientId(id: String, clientId: String): GrantRequest? {
        return grantRequestRepository.getByIdAndClientId(id, clientId)
    }

    override suspend fun save(grantRequest: GrantRequest): GrantRequest {
        return grantRequestRepository.save(GrantRequestEntity(grantRequest)).awaitFirst()
    }

    override suspend fun getGrantRequestByCodeAndClientId(code: String, clientId: String): GrantRequest? {
        return grantRequestRepository.getByCodeAndAndClientId(code, clientId)
    }

    override suspend fun getGrantRequestById(id: String, appUserId: String): GrantRequest? {
        return grantRequestRepository.getByIdAndAssociatedUserId(id, appUserId)
    }
}

