package com.example.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoogleOAuthAccessTokenServiceTest {

    @Test
    void returnsStoredTokenWhileItIsStillValid() throws Exception {
        OAuth2TokenStore store = new OAuth2TokenStore();
        OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);
        store.put("admin@fpt.edu.vn", "current-token", Instant.now().plusSeconds(600),
                "refresh", "google", "principal-1");

        GoogleOAuthAccessTokenService service = new GoogleOAuthAccessTokenService(store, manager);

        assertEquals("current-token", service.getValidAccessToken("ADMIN@fpt.edu.vn"));
        verify(manager, never()).authorize(any());
    }

    @Test
    void refreshesAndStoresExpiredToken() throws Exception {
        OAuth2TokenStore store = new OAuth2TokenStore();
        OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);
        store.put("admin@fpt.edu.vn", "expired", Instant.now().minusSeconds(60),
                "old-refresh", "google", "principal-1");

        ClientRegistration registration = googleRegistration();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "refreshed-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Set.of("openid"));
        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken("new-refresh", Instant.now());
        when(manager.authorize(any())).thenReturn(
                new OAuth2AuthorizedClient(registration, "principal-1", accessToken, refreshToken));

        GoogleOAuthAccessTokenService service = new GoogleOAuthAccessTokenService(store, manager);

        assertEquals("refreshed-token", service.getValidAccessToken("admin@fpt.edu.vn"));
        assertEquals("refreshed-token", store.get("admin@fpt.edu.vn").accessToken);
        assertEquals("new-refresh", store.get("admin@fpt.edu.vn").refreshToken);
        verify(manager).authorize(any());
    }

    @Test
    void explainsWhenExpiredSessionCannotBeRefreshed() {
        OAuth2TokenStore store = new OAuth2TokenStore();
        OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);
        store.put("admin@fpt.edu.vn", "expired", Instant.now().minusSeconds(60),
                null, "google", "principal-1");

        GoogleOAuthAccessTokenService service = new GoogleOAuthAccessTokenService(store, manager);

        assertThrows(GoogleOAuthAccessTokenService.TokenUnavailableException.class,
                () -> service.getValidAccessToken("admin@fpt.edu.vn"));
        verify(manager, never()).authorize(any());
    }

    private ClientRegistration googleRegistration() {
        return ClientRegistration.withRegistrationId("google")
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName("sub")
                .clientName("Google")
                .build();
    }
}
