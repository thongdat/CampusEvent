package com.example.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lưu Google OAuth access_token theo email user, để sau khi đăng nhập
 * có thể gọi Google Forms/Drive API trên danh nghĩa user đó.
 *
 * In-memory (mất khi restart) — đủ cho demo. Nếu cần persist thì
 * lưu vào DB hoặc Redis.
 */
@Component
public class OAuth2TokenStore {

    public static class TokenInfo {
        public final String accessToken;
        public final Instant expiresAt;
        public final String refreshToken;
        public final String registrationId;
        public final String principalName;

        public TokenInfo(String accessToken, Instant expiresAt, String refreshToken) {
            this(accessToken, expiresAt, refreshToken, null, null);
        }

        public TokenInfo(String accessToken, Instant expiresAt, String refreshToken,
                         String registrationId, String principalName) {
            this.accessToken = accessToken;
            this.expiresAt = expiresAt;
            this.refreshToken = refreshToken;
            this.registrationId = registrationId;
            this.principalName = principalName;
        }

        public boolean isValid() {
            return accessToken != null && !accessToken.isBlank()
                    && (expiresAt == null || expiresAt.isAfter(Instant.now().plusSeconds(30)));
        }
    }

    private final Map<String, TokenInfo> tokens = new ConcurrentHashMap<>();

    public void put(String email, String accessToken, Instant expiresAt, String refreshToken) {
        TokenInfo current = get(email);
        put(email, accessToken, expiresAt, refreshToken,
                current == null ? null : current.registrationId,
                current == null ? null : current.principalName);
    }

    public void put(String email, String accessToken, Instant expiresAt, String refreshToken,
                    String registrationId, String principalName) {
        if (email == null || accessToken == null) return;
        tokens.put(email.toLowerCase(Locale.ROOT),
                new TokenInfo(accessToken, expiresAt, refreshToken, registrationId, principalName));
    }

    public TokenInfo get(String email) {
        if (email == null) return null;
        return tokens.get(email.toLowerCase(Locale.ROOT));
    }

    public void remove(String email) {
        if (email == null) return;
        tokens.remove(email.toLowerCase(Locale.ROOT));
    }
}
