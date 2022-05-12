package hu.nagygm.server.security

import hu.nagygm.oauth2.server.client.AppUserDao
import hu.nagygm.server.mangement.appuser.mongo.MongoAppUserRepository
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono


interface UserService : ReactiveUserDetailsService {
    suspend fun getCurrentUser(): User?
}

class MongoUserService(@Autowired val mongoAppUserRepository: MongoAppUserRepository) : UserService {

    override suspend fun getCurrentUser(): User? {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map<User>(this::currentUser).awaitFirstOrNull()
    }

    private fun currentUser(auth: Authentication?): User? {
        return auth?.principal as User?
    }

    override fun findByUsername(username: String?): Mono<UserDetails> {
        requireNotNull(username)
        return mono {
            val appUser = mongoAppUserRepository.findByUsername(username)
            User(
                appUser.username,
                appUser.passwordHash,
                appUser.enabled,
                appUser.accountNonExpired,
                appUser.credentialsNonExpired,
                appUser.accountNonLocked,
                emptyList()
            )
        }
    }

}

@Service
class R2dbcUserService(@Autowired val appUserDao: AppUserDao) : UserService {

    override suspend fun getCurrentUser(): User? {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map<User>(this::currentUser).awaitFirstOrNull()
    }

    private fun currentUser(auth: Authentication?): User? {
        return auth?.principal as User?
    }

    override fun findByUsername(username: String?): Mono<UserDetails> {
        requireNotNull(username)
        return appUserDao.findByUsername(username).map {
            User(
                it.username,
                it.passwordHash,
                it.enabled,
                it.accountNonExpired,
                it.credentialsNonExpired,
                it.accountNonLocked,
                emptyList()
            )
        }
    }

}