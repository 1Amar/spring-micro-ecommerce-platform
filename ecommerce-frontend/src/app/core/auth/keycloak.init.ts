import { KeycloakService } from 'keycloak-angular';
import { environment } from '@environments/environment';

export function initializeKeycloak(keycloak: KeycloakService): () => Promise<boolean> {
  return () =>
    keycloak.init({
      config: {
        url: environment.keycloak.url,
        realm: environment.keycloak.realm,
        clientId: environment.keycloak.clientId,
      },
      initOptions: {
        onLoad: 'check-sso',
        checkLoginIframe: false,
        checkLoginIframeInterval: 0,
        flow: 'standard',
        pkceMethod: 'S256',
        enableLogging: false,
        silentCheckSsoRedirectUri: undefined,
        silentCheckSsoFallback: false
      },
      enableBearerInterceptor: true,
      bearerPrefix: 'Bearer',
      loadUserProfileAtStartUp: false
    });
}