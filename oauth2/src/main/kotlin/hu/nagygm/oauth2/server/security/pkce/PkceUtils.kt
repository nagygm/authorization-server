package hu.nagygm.oauth2.server.security.pkce

import hu.nagygm.oauth2.server.handler.TokenHandler
import org.apache.commons.logging.LogFactory
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.regex.Pattern

/**
 * https://datatracker.ietf.org/doc/html/rfc7636#section-6.1
 */
enum class CodeChallengeMethods(val value: String) {
    PLAIN("plain"),
    S256("s256");

    companion object {
        fun isValid(value: String): Boolean {
            return value.equals(PLAIN.value, true) || value.equals(S256.value, true)
        }

        fun fromString(value: String): CodeChallengeMethods {
            return when (value.lowercase()) {
                PLAIN.value -> PLAIN
                S256.value -> S256
                else -> throw IllegalArgumentException("Invalid code challenge method: $value")
            }
        }

        fun equals(method: CodeChallengeMethods, value: String): Boolean {
            return method.value.equals(value, true)
        }
    }
}

class CodeVerifier {
    companion object {
        private val logger = LogFactory.getLog(TokenHandler::class.java)
        val CODE_CHALLENGE_PATTERN = Pattern.compile("^[0-9a-zA-Z\\-\\.~_]{43,128}$")

        fun processCodeChallenge(codeVerifier: String): String {
            return try {
                val sha256Digester = MessageDigest.getInstance("SHA-256")
                sha256Digester.update(codeVerifier.toByteArray(StandardCharsets.UTF_8))
                val digestBytes = sha256Digester.digest()
                Base64.getUrlEncoder().withoutPadding().encodeToString(digestBytes)
            } catch (e: NoSuchAlgorithmException) {
                logger.error("SHA256 is not supported on the platform, using plain instead plain", e)
                codeVerifier
            }
        }
    }

}