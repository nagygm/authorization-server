package hu.nagygm.oauth2.server.service

interface ConsentService {
    suspend fun createConsent(grantRequestId: String, clientId: String): ConsentPageResponse
    suspend fun processConsent(grantRequestId: String, accept: Boolean, acceptedScopes: Set<String>): ConsentProcessResponse
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