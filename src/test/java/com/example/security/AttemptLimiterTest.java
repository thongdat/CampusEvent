package com.example.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test cho bộ chống brute-force khi đăng nhập / nhập OTP.
 *
 * Ví dụ WHITE-BOX + Boundary Value Analysis quanh ngưỡng MAX_ATTEMPTS = 5:
 * 4 lần sai -> vẫn cho thử; đủ 5 lần sai -> khóa tạm.
 */
@DisplayName("Bảo mật - Chống brute-force đăng nhập/OTP")
class AttemptLimiterTest {

    @Test
    @DisplayName("Tài khoản mới chưa từng sai -> không bị khóa")
    void freshKeyNotLocked() {
        AttemptLimiter limiter = new AttemptLimiter();
        assertFalse(limiter.isLocked("login:new@uni.edu.vn"));
        assertEquals(0, limiter.lockedSeconds("login:new@uni.edu.vn"));
    }

    @Test
    @DisplayName("Sai 4 lần (dưới ngưỡng) -> vẫn chưa bị khóa")
    void belowThresholdNotLocked() {
        AttemptLimiter limiter = new AttemptLimiter();
        String key = "login:user@uni.edu.vn";
        for (int i = 0; i < AttemptLimiter.MAX_ATTEMPTS - 1; i++) {
            limiter.recordFailure(key);
        }
        assertFalse(limiter.isLocked(key));
    }

    @Test
    @DisplayName("Sai đủ 5 lần (đạt ngưỡng) -> bị khóa tạm")
    void reachThresholdLocks() {
        AttemptLimiter limiter = new AttemptLimiter();
        String key = "login:user@uni.edu.vn";
        for (int i = 0; i < AttemptLimiter.MAX_ATTEMPTS; i++) {
            limiter.recordFailure(key);
        }
        assertTrue(limiter.isLocked(key));
        assertTrue(limiter.lockedSeconds(key) > 0);
    }

    @Test
    @DisplayName("Đăng nhập thành công (reset) -> gỡ khóa ngay")
    void resetUnlocks() {
        AttemptLimiter limiter = new AttemptLimiter();
        String key = "login:user@uni.edu.vn";
        for (int i = 0; i < AttemptLimiter.MAX_ATTEMPTS; i++) {
            limiter.recordFailure(key);
        }
        assertTrue(limiter.isLocked(key));

        limiter.reset(key);
        assertFalse(limiter.isLocked(key));
    }

    @Test
    @DisplayName("Key không phân biệt hoa/thường và khoảng trắng thừa")
    void keyIsNormalized() {
        AttemptLimiter limiter = new AttemptLimiter();
        for (int i = 0; i < AttemptLimiter.MAX_ATTEMPTS; i++) {
            limiter.recordFailure("  LOGIN:User@Uni.edu.vn  ");
        }
        assertTrue(limiter.isLocked("login:user@uni.edu.vn"));
    }
}
