package hu.nagygm.oauth2.server.security.twofactor

interface SignInAttemptRepository {
    suspend fun save(signInAttempt: SignInAttempt): SignInAttempt
}


interface SignInAttempt {
    val location: String
    val device: String
    val ip: String
    val time: String
    val type: String
}
