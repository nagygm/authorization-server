package hu.nagygm.server.consent

import hu.nagygm.oauth2.client.registration.GrantRequest
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import java.time.Instant

interface GrantRequestRepository : ReactiveMongoRepository<GrantRequestEntity, String> {
    suspend fun getByIdAndClientId(id: String, clientId: String): GrantRequestEntity
    suspend fun getByCodeAndAndClientId(code: String, clientId: String): GrantRequestEntity
    suspend fun getByIdAndAssociatedUserId(id: String, appUserId: String): GrantRequestEntity
}

@Document
class GrantRequestEntity(
    override val redirectUri: String,
    override val scopes: Set<String>,
    override val responseType: String,
    override val clientId: String,
    @Id
    override var id: String?,
    override var code: String?,
    override val state: String,
    override var codeCreatedAt: Instant?,
    override var requestState: String,
    override var acceptedScopes: Set<String>,
    override var associatedUserId: String?,
    override var consentRequestedAt: Instant?,
    override var processedAt: Instant?
) : GrantRequest {
    constructor(grantRequest: GrantRequest) : this(
        grantRequest.redirectUri,
        grantRequest.scopes,
        grantRequest.responseType,
        grantRequest.clientId,
        grantRequest.id,
        grantRequest.code,
        grantRequest.state,
        grantRequest.codeCreatedAt,
        grantRequest.requestState,
        grantRequest.acceptedScopes,
        grantRequest.associatedUserId,
        grantRequest.consentRequestedAt,
        grantRequest.processedAt
    )
}