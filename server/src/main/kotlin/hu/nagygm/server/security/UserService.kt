package hu.nagygm.server.security

import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Service


@Service
class UserService {

    suspend fun getCurrentUser(): User? {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map<User>(this::currentUser).awaitFirstOrNull()
    }

    private fun currentUser(auth: Authentication?): User? {
        return auth?.principal as User?
    }

}
