package hu.nagygm.oauth2.server.exception

import hu.nagygm.oauth2.server.handler.AuthorizationHandler
import org.springframework.security.oauth2.core.OAuth2AuthorizationException
import org.springframework.security.oauth2.core.OAuth2Error

open class ContextAwareOAuth2AuthorizationException(error: OAuth2Error, val context: AuthorizationHandler.AuthorizationRequest) :
    OAuth2AuthorizationException(error)

class AuthorizationCodeGrantException(error: OAuth2Error, context: AuthorizationHandler.AuthorizationRequest) :
    ContextAwareOAuth2AuthorizationException(error, context) {

    companion object {
        const val oauth2DocumentationURI = "https://datatracker.ietf.org/doc/html/draft-ietf-oauth-v2-1-05#section-4.1.2.1"
    }
}