package hu.nagygm.server.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.PropertySource
import org.springframework.stereotype.Component

@Component
@PropertySource("classpath:application.properties")
class AdminManagementApi {
    @Value("\${management.api.v1:/management/v1}")
    lateinit var management: String

    @Value("\${oauth2.api.v1.clients:/clients}")
    lateinit var clients: String

    @Value("\${oauth2.api.v1.resources:/resources}")
    lateinit var resources: String

    @Value("\${oauth2.api.v1.users:/users}")
    lateinit var users: String

    fun clientsPath(absolute: Boolean = true) = if (absolute) "$management$clients" else clients
    fun resourcesPath(absolute: Boolean = true) = if (absolute) "$management$resources" else resources
    fun usersPath(absolute: Boolean = true) = if (absolute) "$management$users" else users
}