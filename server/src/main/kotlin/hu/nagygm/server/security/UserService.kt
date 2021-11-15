package hu.nagygm.server.security

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


@Service
class UserService(@Autowired val appUserRepository: AppUserRepository) : ReactiveUserDetailsService {

    suspend fun getCurrentUser(): User? {
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
            val appUser = appUserRepository.findByUsername(username)
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
