package hu.nagygm.server

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class AuthorizationServerApplication


fun main(args: Array<String>) {
    SpringApplication.run(AuthorizationServerApplication::class.java, *args)
}