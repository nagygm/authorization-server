package hu.nagygm.server.security

import org.springframework.data.mongodb.core.mapping.Document

@Document
data class UserPrincipal(
    val id: String,
    val accessToken: String
)
