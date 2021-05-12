package hu.nagygm.server.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers

@Configuration
open class BasicSecurityConfiguration {

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain? {
        http
            .authorizeExchange()
            .anyExchange().permitAll()
            .and().formLogin()
//            .and().csrf().requireCsrfProtectionMatcher(
//                ServerWebExchangeMatcher { exchange ->
//                    ServerWebExchangeMatchers.pathMatchers("/urls-with-csrf-check/**")
//                        .matches(exchange)
//                }
//            )
            .and().httpBasic()
        return http.build()
    }

    @Bean
    fun userDetailsService(): MapReactiveUserDetailsService {
        val user: UserDetails = User.withDefaultPasswordEncoder()
            .username("user").password("user").roles("USER")
            .build()
        return MapReactiveUserDetailsService(user)
    }

}
