package hu.nagygm.oauth2.client.registration


interface ClientRegistrationRepository {
    suspend fun findById(id: String): ClientRegistration?

    suspend fun findByClientId(clientId: String): ClientRegistration?

    suspend fun findByClientIdAndSecret(clientId: String, secret: String): ClientRegistration?

    suspend fun save(clientRegistration: ClientRegistration): ClientRegistration
}
