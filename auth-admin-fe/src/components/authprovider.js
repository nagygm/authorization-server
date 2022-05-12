import base64url from 'base64url';
import { AUTH_LOGIN, AUTH_LOGOUT, AUTH_ERROR, AUTH_CHECK } from 'react-admin';
import { UserManager } from 'oidc-client';

const issuer = 'http://localhost:8081';
const clientId = 'postman-client';
const redirectUri = "http://localhost:3000/login";
const apiUri = "http://localhost:8081/oauth2/v1";

const userManager = new UserManager({
    authority: issuer,
    client_id: clientId,
    redirect_uri: redirectUri,
    response_type: 'code',
    scope: 'scope1 scope2',
    metadata: {
        issuer : issuer,
        authorization_endpoint: "http://localhost:8081/oauth2/v1/authorize",
        token_endpoint: "http://localhost:8081/oauth2/v1/token",
        token_endpoint_auth_methods_supported: "client_secret_basic",
        response_types_supported: ["code", "token"],
        scopes_supported: ["scope1 scope2 scope3"],
        code_challenge_methods_supported: ["plain", "S256"],
        claims_supported: ["aud"],
        token_endpoint_auth_signing_alg_values_supported: ["hs256"],
        grant_types_supported: ["authorization_code", "client_credentials", "refresh_token"],
    }
});

const cleanup = () => {
    window.history.replaceState(
        {},
        window.document.title,
        window.location.origin
    );
}
function getProfileFromToken(jsonToken) {
    const token = JSON.parse(jsonToken);
    const jwt = JSON.parse(base64url.decode(token.id_token.split('.')[1]));
    console.log(jwt)

    return { id: 'my-profile', ...jwt }
}

export const signinRedirect = () => {
    userManager.signinRedirect();
    return;
}

export const authProvider = async (type, params = {}) => {
    if (type === AUTH_LOGIN) {
        if (!params.code || !params.state) {
            userManager.signinRedirect().then(
            );
            return;
        }

        const stateKey = `oidc.${params.state}`;
        const { code_verifier } = JSON.parse(
            localStorage.getItem(stateKey) || '{}'
        );

        // Transform the code to a token via the API
        const response = await fetch('http://localhost:8081/oauth2/v1/token', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ code: params.code, code_verifier }),
        });

        if (!response.ok) {
            cleanup();
            return Promise.reject();
        }

        const token = await response.json();

        localStorage.setItem('token', JSON.stringify(token));
        userManager.clearStaleState();
        cleanup();
        return Promise.resolve();
    }

    if ([AUTH_LOGOUT, AUTH_ERROR].includes(type)) {
        localStorage.removeItem('token');
        return Promise.resolve();
    }

    if (type === AUTH_CHECK) {
        const token = localStorage.getItem('token');

        if (!token) {
            return Promise.reject()
        }

        const jwt = getProfileFromToken(token);
        const now = new Date();

        return now.getTime() > (jwt.exp * 1000) ? Promise.reject() : Promise.resolve()
    }

    return Promise.resolve();
}
