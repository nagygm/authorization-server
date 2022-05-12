package hu.nagygm.oauth2.server.service

import hu.nagygm.oauth2.server.handler.AuthorizationHandler
import hu.nagygm.oauth2.util.State
import java.time.Instant

interface GrantRequestService {
    suspend fun saveGrantRequest(request: AuthorizationHandler.AuthorizationRequest): GrantRequest
    suspend fun getGrantRequestByIdAndClientId(id: String, clientId: String): GrantRequest?
    suspend fun save(grantRequest: GrantRequest): GrantRequest
    suspend fun getGrantRequestByCodeAndClientId(code: String, clientId: String): GrantRequest?
    suspend fun getGrantRequestById(id: String, appUserId: String): GrantRequest?
}

interface GrantRequest {
    val redirectUri: String
    val scopes: Set<String>
    val responseType: String
    val clientId: String
    var id: String?
    var code: String?
    val state: String
    var codeCreatedAt: Instant?
    var requestState: String
    var acceptedScopes: Set<String>
    var associatedUserId: String?
    var processedAt: Instant?
    var consentRequestedAt: Instant?
    var codeChallenge: String?
    var codeChallengeMethod: String?
}

sealed class GrantRequestStates(override val code: String) : State {
    object Created: GrantRequestStates("created")
    object ConsentRequested: GrantRequestStates("consent_requested")
    object ConsentRejected: GrantRequestStates("consent_rejected")
    object ConsentAccepted: GrantRequestStates("consent_accepted")
}
