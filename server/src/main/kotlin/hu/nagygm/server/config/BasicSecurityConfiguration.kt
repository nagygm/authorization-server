package hu.nagygm.server.config

import hu.nagygm.oauth2.config.annotation.OAuth2AuthorizationServerEndpointConfiguration.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers


@Configuration
class BasicSecurityConfiguration {

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain? {
        http
            .csrf().requireCsrfProtectionMatcher(NegatedServerWebExchangeMatcher { exchange ->
                ServerWebExchangeMatchers.pathMatchers(
                    "${basePathV1.oauth2}/**", "/login**", "/consent**", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/swagger-ui/**", "/swagger-ui.html", *tempAuthOverride()
                ).matches(exchange)
            }).and()
            .authorizeExchange()
            .pathMatchers("${basePathV1.oauth2}/**", "/favicon.ico", "/login", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/swagger-ui/**", "/swagger-ui.html", *tempAuthOverride())
            .permitAll().and()
            .authorizeExchange()
            .anyExchange().authenticated()
            .and().formLogin()
            .loginPage("/login")
            .authenticationSuccessHandler(redirectSuccessHandler()).and().logout()
            .and().headers().referrerPolicy()
            .policy(ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.ORIGIN_WHEN_CROSS_ORIGIN)
        return http.build()
    }

    private fun redirectSuccessHandler(): RedirectServerAuthenticationSuccessHandler {
        return RedirectServerAuthenticationSuccessHandler()
    }

    private fun tempAuthOverride() : Array<String> {
        //FIXME: remove this after role implementation
        return arrayOf("${basePathV1.management}/**")
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
