package hu.nagygm.oauth2.client

interface ClientRegistrationService {
    suspend fun findById(id: String): ClientRegistration

    suspend fun findByClientId(clientId: String): ClientRegistration
}
