package hu.nagygm.oauth2.client.registration

import hu.nagygm.oauth2.server.web.AuthorizationHandler
import java.time.Instant

interface GrantRequestService {
    suspend fun saveGrantRequest(request: AuthorizationHandler.AuthorizationRequest): GrantRequest
    suspend fun getGrantRequestByIdAndClientId(id: String, clientId: String): GrantRequest
    suspend fun save(grantRequest: GrantRequest): GrantRequest
    suspend fun getGrantRequestByCodeAndClientId(code: String, clientId: String): GrantRequest
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
}
