package hu.nagygm.oauth2.core

//enum class AuthorizationGrantType(val code: String) {
//    AUTHORIZATION_CODE("authorization_code"),
//    IMPLICIT("implicit"),
//    REFRESH_TOKEN("refresh_token"),
//    CLIENT_CREDENTIALS("client_credentials"),
//    PASSWORD("password");
//}

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

enum class ChallengeMethods(val value: String) {
    PLAIN("plain"),
    S256("s256"),
}

enum class TokenType(val value: String) {
    ACCESS_TOKEN("access_token"),
    REFRESH_TOKEN("refresh_token")
}
