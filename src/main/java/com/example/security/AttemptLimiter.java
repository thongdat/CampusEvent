package com.example.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bộ giới hạn số lần thử (đăng nhập, nhập OTP...) để chống brute-force.
 * Lưu trạng thái trong bộ nhớ (đủ cho ứng dụng chạy một node). Mỗi "key"
 * (vd "login:email", "otp:email") có bộ đếm thất bại riêng; vượt ngưỡng sẽ
 * bị khoá tạm thời trong một khoảng thời gian.
 */
@Component
public class AttemptLimiter {

    /** Số lần thất bại tối đa trước khi khoá tạm. */
    public static final int MAX_ATTEMPTS = 5;
    /** Thời gian khoá sau khi vượt ngưỡng (phút). */
    public static final int LOCK_MINUTES = 15;

    private static final class Counter {
        int failures;
        LocalDateTime lockedUntil;
    }

    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();

    /** @return số giây còn bị khoá; 0 nếu không bị khoá. */
    public long lockedSeconds(String key) {
        Counter counter = counters.get(normalize(key));
        if (counter == null || counter.lockedUntil == null) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(counter.lockedUntil)) {
            return 0;
        }
        return Math.max(1, Duration.between(now, counter.lockedUntil).getSeconds());
    }

    public boolean isLocked(String key) {
        return lockedSeconds(key) > 0;
    }

    /** Ghi nhận một lần thất bại; tự khoá khi vượt ngưỡng. */
    public void recordFailure(String key) {
        String k = normalize(key);
        counters.compute(k, (ignored, existing) -> {
            Counter counter = existing != null ? existing : new Counter();
            LocalDateTime now = LocalDateTime.now();
            if (counter.lockedUntil != null && now.isAfter(counter.lockedUntil)) {
                counter.failures = 0;
                counter.lockedUntil = null;
            }
            counter.failures++;
            if (counter.failures >= MAX_ATTEMPTS) {
                counter.lockedUntil = now.plusMinutes(LOCK_MINUTES);
            }
            return counter;
        });
    }

    /** Xoá bộ đếm khi thao tác thành công. */
    public void reset(String key) {
        counters.remove(normalize(key));
    }

    private String normalize(String key) {
        return key == null ? "" : key.trim().toLowerCase();
    }
}
