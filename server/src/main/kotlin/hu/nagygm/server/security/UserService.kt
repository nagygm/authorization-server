package hu.nagygm.server.security

import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Service
import java.util.Optional


@Service
class UserService {

    suspend fun getCurrentUser() {
        ReactiveSecurityContextHolder.getContext()
            .map<Authentication>(SecurityContext::getAuthentication)
            .map<Any>(this::currentUser).awaitFirstOrNull()
    }

    private fun currentUser(auth: Authentication?): Optional<User> {
        if (auth != null) {
            val principal = auth.principal
            if (principal is User) {
                return Optional.of(principal)
            }
        }
        return Optional.empty()
    }


}
