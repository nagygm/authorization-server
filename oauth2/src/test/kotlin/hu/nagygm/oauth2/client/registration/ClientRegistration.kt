import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.extensions.spring.SpringExtension

class ClientRegistrationSpecification : AnnotationSpec() {
    override fun extensions() = listOf(SpringExtension)

    // Global
    //Must use TLS
    // The endpoint MUST NOT include a fragment component


    //Sectino 2

    @Test
    fun `Before initiating the protocol, the client must establish its registration with the authorization server`() {
        //From RFC 6749:
    }

    @Test
    fun `When unknown client tries to use the service return with "invalid_client"`() {
        //
    }

    @Test
    fun `During client registration the client type must be specified`() {
        //From RFC 6749:

    }

    @Test
    fun `Every Client Type must have redirect URI`() {

    }

    @Test
    fun `Necessary information check for authoriztaion server`() {

    }

    @Test
    fun `Confidential clients must have credentials, and their identity confirmed`() {

    }

    @Test
    fun `Credentialed clients must have credentials`() {

    }

    @Test
    fun `Public credentials does not need public clients`() {

    }

    @Test
    fun `A single "client_id" MUST NOT be treated as more than one type of client`() {

    }

    @Test
    fun `The client identifier must be unique to the authorization server`() {

    }

    @Test
    fun `Client idetifier must not be used alone for client authentication`() {

    }

    @Test
    fun `The authorization server SHOULD document the size of any identifier it issues`() {

    }

    @Test
    fun `The AS should not allow clients to choose or influence ther "client_id" value`() {

    }

    @Test
    fun `The redirect URI MUST be an absoule URI as defined by RFC3986`() {

    }

    @Test
    fun `The redirect URI may inclue an "application x-ww-form-urlencoded" formatted query component, MUST be retained with other qzery components`() {

    }

    @Test
    fun `The redirect URI MUST NOT include a fragment component`() {

    }

    @Test
    fun `The redirection endpoint SHOULD require the use of TLS as described in Section 1DOT5 when the requested response type is "code"`() {

    }

    @Test
    fun `The redirection endpoint SHOULD require the use of TLS or when the redirection request will result in the transmission of sensitive credentials over an open network`() {

    }

    @Test
    fun `Authorization servers MUST require clients to register their complete redirect URI (including the path component) and reject authorization requests that specify a redirect URI that doesn't exactly match one that was registered`() {

    }

    @Test
    fun `the exception is loopback redirects, where an exact match is required except for the port URI component`() {

    }

    @Test
    fun `For private-use URI scheme-based redirect URIs, authorization servers SHOULD enforce the requirement in Section 8DOT3DOT1 that clients use schemes that are reverse domain name based`() {

    }

    @Test
    fun `At a minimum, any private-use URI scheme that doesn't contain a period character ("DOT") SHOULD be rejected`() {

    }

    @Test
    fun `The client MAY use the "state" request parameter to achieve per-request customization if needed rather than varying the redirect URI per request`() {

    }

    @Test
    fun `The authorization server MAY allow the client to register multiple redirect URIs`() {

    }

    @Test
    fun `If multiple redirect URIs have been registered, the client MUST include a redirect URI with the authorization request using the "redirect_uri" request parameter`() {

    }

    @Test
    fun `If an authorization request fails validation due to a missing, invalid, or mismatching redirect URI, the authorization server SHOULD inform the resource owner of the error and MUST NOT automatically redirect the user agent to the invalid redirect URI`() {

    }

    @Test
    fun `The authorization server MUST authenticate the client whenever possible`() {

    }

    @Test
    fun `It is RECOMMENDED to use asymmetric (public-key based) methods for client authentication such as mTLS RFC8705 or "private_key_jwt" OpenID`() {

    }

    @Test
    fun `If the authorization server cannot authenticate the client due to the client's nature, the authorization server SHOULD utilize other means to protect resource owners from such potentially malicious clients For example, the authorization server can engage the resource owner to assist in identifying the client and its origin`() {

    }

    @Test
    fun `The authorization server MAY establish a client authentication method with public clients, which converts them to credentialed clients However, the authorization server MUST NOT rely on credentialed client authentication for the purpose of identifying the client`() {

    }

    @Test
    fun `The client MUST NOT use more than one authentication method in each request`() {

    }

    @Test
    fun `Clients in possession of a client secret, sometimes known as a client password, MAY use the HTTP Basic authentication scheme as defined in RFC7235 to authenticate with the authorization server`() {

    }

    @Test
    fun `The authorization server MUST support the HTTP Basic authentication scheme for authenticating clients that were issued a client secret`() {

    }

    @Test
    fun `The authorization server MUST require the use of TLS as described in Section 1DOT5 when sending requests using password authentication`() {

    }

    @Test
    fun `the authorization server MUST protect any endpoint utilizing it against brute force attacks`() {

    }

    @Test
    fun `When using other authentication methods, the authorization server MUST define a mapping between the client identifier (registration record) and authentication scheme`() {

    }

    @Test
    fun `The AS may support including client credentials in request-body using POST, required client_id and client_secret, must not be included in URI`() {

    }

    @Test
    fun `The clients must be registered beforehand to be able to use the authorization server`() {

    }



}
