package com.example.security;

import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.stereotype.Service;

/** Resolves a valid Google token and refreshes it when possible. */
@Service
public class GoogleOAuthAccessTokenService {

    private final OAuth2TokenStore tokenStore;
    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public GoogleOAuthAccessTokenService(OAuth2TokenStore tokenStore,
                                         OAuth2AuthorizedClientManager authorizedClientManager) {
        this.tokenStore = tokenStore;
        this.authorizedClientManager = authorizedClientManager;
    }

    public String getValidAccessToken(String email) throws TokenUnavailableException {
        OAuth2TokenStore.TokenInfo token = tokenStore.get(email);
        if (token == null) {
            throw new TokenUnavailableException(
                    "Chưa có phiên Google cho tài khoản này. Hãy đăng xuất rồi đăng nhập bằng Google.");
        }
        if (token.isValid()) {
            return token.accessToken;
        }
        if (isBlank(token.registrationId) || isBlank(token.principalName) || isBlank(token.refreshToken)) {
            throw new TokenUnavailableException(
                    "Phiên Google đã hết hạn và không có refresh token. Hãy đăng nhập lại bằng Google.");
        }

        try {
            OAuth2AuthorizedClient client = authorizedClientManager.authorize(
                    OAuth2AuthorizeRequest.withClientRegistrationId(token.registrationId)
                            .principal(token.principalName)
                            .build());
            OAuth2AccessToken accessToken = client == null ? null : client.getAccessToken();
            if (accessToken == null || accessToken.getTokenValue() == null) {
                throw new TokenUnavailableException(
                        "Google không gia hạn được phiên đăng nhập. Hãy đăng nhập lại bằng Google.");
            }
            OAuth2RefreshToken refreshToken = client.getRefreshToken();
            tokenStore.put(email,
                    accessToken.getTokenValue(),
                    accessToken.getExpiresAt(),
                    refreshToken != null ? refreshToken.getTokenValue() : token.refreshToken,
                    token.registrationId,
                    token.principalName);
            return accessToken.getTokenValue();
        } catch (TokenUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TokenUnavailableException(
                    "Không thể gia hạn quyền Google. Hãy đăng xuất rồi đăng nhập lại bằng Google.", ex);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static class TokenUnavailableException extends Exception {
        public TokenUnavailableException(String message) {
            super(message);
        }

        public TokenUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
