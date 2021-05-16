package hu.nagygm.server.consent

import hu.nagygm.oauth2.client.registration.GrantRequest
import hu.nagygm.oauth2.client.registration.GrantRequestService
import hu.nagygm.oauth2.server.web.AuthorizationHandler
import hu.nagygm.server.security.AppUserRepository
import hu.nagygm.server.security.UserService
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class ConsentServiceImpl(
    @Autowired val userService: UserService,
    @Autowired val consentRepository: ConsentRepository,
    @Autowired val appUserRepository: AppUserRepository,
    @Autowired val grantRequestService: GrantRequestService,
    @Autowired val reactiveMongoTemplate: ReactiveMongoTemplate
) : ConsentService {

    private val codeGenerator = Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 96)

    override suspend fun createConsent(grantRequestId: String, clientId: String): ConsentEntity {
        val user = userService.getCurrentUser() ?: throw AccessDeniedException("Access denied: can't find user")
        val appUserDetails = appUserRepository.findByUsername(user.username)
        val granRequest = grantRequestService.getGrantRequestByIdAndClientId(grantRequestId, clientId)
        return reactiveMongoTemplate.insert(
            ConsentEntity(
                null,
                granRequest.scopes,
                granRequest.clientId,
                appUserDetails.id,
                Instant.now(),
                null,
                null,
                granRequest.redirectUri,
                granRequest.id!!
            )
        ).awaitFirst()
    }

    override suspend fun processConsent(id: String, accept: Boolean): ConsentResponse {
        val user = userService.getCurrentUser() ?: throw AccessDeniedException("Access denied: can't find user")
        val appUserDetails = appUserRepository.findByUsername(user.username)
        val consent = consentRepository.findByIdAndUserId(id, appUserDetails.id)
        consent.accepted = accept
        consent.answeredAt = Instant.now()
        consentRepository.save(consent).awaitFirst()
        val grantRequest = grantRequestService.getGrantRequestByIdAndClientId(consent.grantRequestId, consent.clientId)
        return if (accept) {
            grantRequest.code = codeGenerator.generateKey()
            grantRequest.codeCreatedAt = Instant.now()
            grantRequestService.save(grantRequest)
            ConsentResponse("${consent.redirectUrl}?code=${grantRequest.code}&state=${grantRequest.state}")
        } else {
            ConsentResponse("${consent.redirectUrl}?error=${OAuth2ErrorCodes.ACCESS_DENIED}")
        }
    }

}

interface ConsentService {
    suspend fun createConsent(grantRequestId: String, clientId: String): ConsentEntity
    suspend fun processConsent(id: String, accept: Boolean): ConsentResponse
}

interface ConsentRepository : ReactiveMongoRepository<ConsentEntity, String> {
    suspend fun findByIdAndUserId(id: String, userId: String): ConsentEntity
}

@Document
data class ConsentEntity(
    @Id
    var id: String?,
    val scopes: Set<String>,
    val clientId: String,
    val userId: String,
    val createdAt: Instant,
    var answeredAt: Instant?,
    var accepted: Boolean?,
    val redirectUrl: String,
    val grantRequestId: String
)

data class ConsentResponse(
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
                null
            )
        ).awaitFirst()
    }

    override suspend fun getGrantRequestByIdAndClientId(id: String, clientId: String): GrantRequest {
        return grantRequestRepository.getByIdAndClientId(id, clientId)
    }

    override suspend fun save(grantRequest: GrantRequest): GrantRequest {
        return grantRequestRepository.save(GrantRequestEntity(grantRequest)).awaitFirst()
    }

    override suspend fun getGrantRequestByCodeAndClientId(code: String, clientId: String): GrantRequest {
        return grantRequestRepository.getByCodeAndAndClientId(code, clientId)
    }

}

