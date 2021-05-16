package hu.nagygm.server

import hu.nagygm.oauth2.config.annotation.EnableOauth2AuthorizationServer
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
@EnableOauth2AuthorizationServer
class AuthorizationServerApplication

fun main(args: Array<String>) {
    SpringApplication.run(arrayOf(AuthorizationServerApplication::class.java), args)
}
