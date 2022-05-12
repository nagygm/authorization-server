package hu.nagygm.server.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers


@EnableWebFluxSecurity
@Configuration
class BasicSecurityConfiguration {

    @Autowired lateinit var oauth2RequestMatcher: ServerWebExchangeMatcher
    @Autowired lateinit var oauth2ConsentRequestMatcher: ServerWebExchangeMatcher

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http.cors().disable().csrf()
            .requireCsrfProtectionMatcher(NegatedServerWebExchangeMatcher { exchange ->
                ServerWebExchangeMatchers.matchers(
                    ServerWebExchangeMatchers.pathMatchers(
                        "/swagger-ui/**", "/v3/api-docs/**", "/webjars/swagger-ui/**", "/swagger-ui.html", "/actuator/**",
                        "/login"
                    ),
                    ServerWebExchangeMatchers.matchers(oauth2RequestMatcher, oauth2ConsentRequestMatcher)
                ).matches(exchange)
            }).and()
            .authorizeExchange().matchers(
                ServerWebExchangeMatchers.pathMatchers(
                    "/favicon.ico", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/swagger-ui/**", "/swagger-ui.html",
                    "/actuator/**", "/login**"
                ),
                oauth2RequestMatcher
            ).permitAll()
            .and().formLogin()
//            .loginPage("/login")
            .authenticationSuccessHandler(redirectSuccessHandler()).and().logout()
            .and().oauth2Login()
            .and().authorizeExchange()
            .anyExchange().permitAll()
            .authenticated()
            .and().headers().referrerPolicy()
            .policy(ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.ORIGIN_WHEN_CROSS_ORIGIN)
        return http.build()
    }

    @Bean
    fun clientRegistrationRepository(): ReactiveClientRegistrationRepository? {
        return InMemoryReactiveClientRegistrationRepository(authserverClientRegistration())
    }

    private fun authserverClientRegistration(): ClientRegistration {
        return ClientRegistration.withRegistrationId("authserver")
            .clientId("postman-client")
            .clientSecret("postman-client")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost:8081/login/oauth2/code/{registrationId}")
            .scope("scope1", "scope2")
            .authorizationUri("http://localhost:8081/oauth2/v1/authorize")
            .tokenUri("http://localhost:8081/oauth2/v1/token")
            .userInfoUri("http://localhost:8081/oauth2/v1/userinfo")
            .userNameAttributeName(IdTokenClaimNames.SUB)
            .clientName("authserver")
            .build()
    }

    private fun redirectSuccessHandler(): RedirectServerAuthenticationSuccessHandler {
        return RedirectServerAuthenticationSuccessHandler()
    }

    private fun tempAuthOverride() : Array<String> {
        //FIXME: remove this after role implementation
        return arrayOf("ADMIN")
    }

//    @Bean
//    fun userDetailsService(): ReactiveUserDetailsService {
//        val user: UserDetails = User.withDefaultPasswordEncoder()
//            .username("user").password("user").roles("USER")
//            .build()
//        return MapReactiveUserDetailsService(user)
//    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        //TODO add dynamic bcrypt strength increase and rehash on login when original strength is low
        return BCryptPasswordEncoder(12)
    }
}
