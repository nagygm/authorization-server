package hu.nagygm.oauth2.server.client

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

interface AppUserDao {

   fun fetchAllUsersWithProfile(): Flux<out AppUserProfileProjection>

   fun fetchOneUsersWithProfile(userId: UUID): Mono<out AppUserProfileProjection>

   fun findByUsername(username: String?) : Mono<out AppUserProjection>
   fun getIdByUsername(username: String?): Mono<out UUID>
}

interface AppUserProfileProjection {
    val userId: UUID
    val username: String
    val firstName: String
    val lastName: String
    val email: String
    val enabled: Boolean
}

interface AppUserProjection {
    val username: String
    val passwordHash: String
    val enabled: Boolean
    val accountNonExpired: Boolean
    val credentialsNonExpired: Boolean
    val accountNonLocked: Boolean
    val mfa: Boolean
}