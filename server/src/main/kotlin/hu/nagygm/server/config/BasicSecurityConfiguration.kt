package hu.nagygm.server.config

import hu.nagygm.oauth2.core.Endpoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
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
                    "/authorize**", "/token**", "/login**", "/consent**"
                ).matches(exchange)
            }).and()
            .authorizeExchange()
            .pathMatchers("${Endpoint.AUTHORIZATION.path}**", "${Endpoint.TOKEN.path}**", "/favicon.ico", "/login")
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
