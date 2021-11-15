package hu.nagygm.oauth2.core

enum class AuthorizationMethod(val code: String) {
    NONE("none"),
    POST("post"),
    BASIC("basic");
}

enum class Endpoint(val path: String) {
    AUTHORIZATION("/authorize"),
    TOKEN("/token"),
    USERINFO("/userinfo"),
    END_SESSION("/endsession"),
    CHECK_SESSION("/checksession"),
    REVOCATION("/revocation"),
    INTROSPECT("/introspect"),
    JWKS_URI("/openid_configuration/jwks");
}

//PkceParameterNames

enum class CodeChallengeMethods(val value: String) {
    PLAIN("plain"),
    S256("s256"),
}

enum class TokenType(val value: String) {
    ACCESS_TOKEN("access_token"),
    REFRESH_TOKEN("refresh_token")
}

enum class AccessTokenType(val value: String) {
    BEARER_TOKEN(""),
    MAC_TOKEN("")
}

// Oauth2ParameterNames
