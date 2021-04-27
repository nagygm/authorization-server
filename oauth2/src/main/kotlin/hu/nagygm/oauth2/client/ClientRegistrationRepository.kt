package hu.nagygm.oauth2.client;

interface ClientRegistrationRepository {
    suspend fun findById(id: String): ClientRegistration

    suspend fun findByClientId
}
