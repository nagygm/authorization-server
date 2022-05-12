package hu.nagygm.oauth2.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.PropertySource
import org.springframework.stereotype.Component

@Component
@PropertySource("classpath:application.properties")
class OAuth2Api {
    @Value("\${oauth2.api.v1.base:/oauth2/v1}")
    final lateinit var oauth2: String
        private set

    @Value("\${oauth2.api.v1.authorize:/authorize}")
    final lateinit var authorize: String
        private set

    @Value("\${oauth2.api.v1.token:/token}")
    final lateinit var token: String
        private set

    @Value("\${oauth2.api.v1.consent:/consent}")
    final lateinit var consent: String
        private set

    @Value("\${oauth2.api.v1.userinfo:/userinfo}")
    final lateinit var userinfo: String
        private set

    @Value("\${oauth2.api.v1.endsession:/endsession}")
    final lateinit var endsession: String
        private set

    @Value("\${oauth2.api.v1.checksession:/checksession}")
    final lateinit var checksession: String
        private set

    @Value("\${oauth2.api.v1.revocation:/revocation}")
    final lateinit var revocation: String
        private set

    @Value("\${oauth2.api.v1.introspect:/introspect}")
    final lateinit var introspect: String
        private set

    fun authorizePath(withBase: Boolean = true) = if (withBase) "$oauth2$authorize" else authorize
    fun tokenPath(withBase: Boolean = true) = if (withBase) "$oauth2$token" else token
    fun consentPath(withBase: Boolean = true) = if (withBase) "$oauth2$consent" else consent
    fun userinfoPath(withBase: Boolean = true) = if (withBase) "$oauth2$userinfo" else userinfo
    fun endsessionPath(withBase: Boolean = true) = if (withBase) "$oauth2$endsession" else endsession
    fun checksessionPath(withBase: Boolean = true) = if (withBase) "$oauth2$checksession" else checksession
    fun revocationPath(withBase: Boolean = true) = if (withBase) "$oauth2$revocation" else revocation
    fun introspectPath(withBase: Boolean = true) = if (withBase) "$oauth2$introspect" else introspect
}