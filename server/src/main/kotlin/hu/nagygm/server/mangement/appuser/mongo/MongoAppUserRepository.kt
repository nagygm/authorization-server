package hu.nagygm.server.mangement.appuser.mongo

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface MongoAppUserRepository : ReactiveMongoRepository<AppUser, String> {
    suspend fun findByUsername(username: String): AppUser
    suspend fun save(appUser: AppUser): AppUser
}

@Document
data class AppUser(
    @Id val id: String,
    val username: String,
    val passwordHash: String,
    val accountNonExpired: Boolean,
    val accountNonLocked: Boolean,
    val credentialsNonExpired: Boolean,
    val enabled: Boolean,
)
