package hu.nagygm.oauth2.client



interface OAuth2AuthorizationRepository {

    suspend fun findById(id: String): OAuth2Authorization?

    suspend fun save(authorization: OAuth2Authorization)

    suspend fun remove(id: String)
}
