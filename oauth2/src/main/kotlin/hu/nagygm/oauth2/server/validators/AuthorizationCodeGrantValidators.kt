package hu.nagygm.oauth2.server.validators

import java.net.URI
import java.net.URISyntaxException

/**
 * T: Object type of the target we want to validate
 * C: Context type
 */
interface ContextAwareValidator<T, C> {
    fun validate(target: T, context: C): Boolean
}

/**
 * Accepts Redirect URI as String, and needs a Set of the registered URIS as Context to validate extra rules
 */
object RedirectUriValidator : ContextAwareValidator<String, Set<String>> {

    override fun validate(target: String, context: Set<String>): Boolean {
        return validateRedirectUri(target, context)
    }

    private fun validateRedirectUri(
        redirectUri: String,
        registeredRedirectUris: Set<String>
    ): Boolean {
        if ((registeredRedirectUris.size != 1 && redirectUri.isBlank()) || (redirectUri.isNotBlank() &&
                    (!validateRedirectUriFormat(redirectUri) || !registeredRedirectUris.contains(redirectUri)))
        ) {
            return true
        }
        return false
    }

    private fun validateRedirectUriFormat(uri: String): Boolean {
        try {
            URI(uri)
        } catch (e: URISyntaxException) {
            return false
        }
        return true
    }

}









