package hu.nagygm.oauth2.server.security.twofactor

data class SignInAttempt(
    val location: String,
    val device: String,
    val ip: String,
    val time: String,
    val type: String
)
