const KEYCLOAK_URL = import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8181'

export const authConfig = {
  clientId: 'oauth2-pkce-client',
  authorizationEndpoint: `${KEYCLOAK_URL}/realms/fitness-oauth2/protocol/openid-connect/auth`,
  tokenEndpoint: `${KEYCLOAK_URL}/realms/fitness-oauth2/protocol/openid-connect/token`,
  redirectUri: import.meta.env.VITE_REDIRECT_URI || 'http://localhost:5173/',
  logoutEndpoint: `${KEYCLOAK_URL}/realms/fitness-oauth2/protocol/openid-connect/logout`,
  logoutRedirect: import.meta.env.VITE_REDIRECT_URI || 'http://localhost:5173/',
  scope: 'openid profile email offline_access',
  onRefreshTokenExpire: (event) => event.logIn(),
}