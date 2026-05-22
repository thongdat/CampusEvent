package com.example.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cho phép frontend kiểm tra OAuth Google đã được cấu hình chưa.
 * Nếu chưa cấu hình, login.html sẽ ẩn nút "Tiếp tục với Google" và
 * hiển thị hướng dẫn ngắn thay vì để người dùng bấm vào và gặp lỗi
 * "invalid_client" của Google.
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class OAuthConfigController {

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
    private String googleClientSecret;

    @GetMapping("/oauth-status")
    public ResponseEntity<Map<String, Object>> oauthStatus() {
        boolean googleEnabled = isConfigured(googleClientId) && isConfigured(googleClientSecret);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("googleEnabled", googleEnabled);
        if (!googleEnabled) {
            body.put("googleHint",
                "Chưa cấu hình Google OAuth. Vào Google Cloud Console tạo OAuth Client ID, "
                + "đặt Authorized redirect URI là http://localhost:8081/api/login/oauth2/code/google, "
                + "rồi gán biến môi trường GOOGLE_CLIENT_ID và GOOGLE_CLIENT_SECRET trước khi chạy lại.");
        }
        return ResponseEntity.ok(body);
    }

    private boolean isConfigured(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        // Coi các giá trị placeholder mặc định là "chưa cấu hình".
        String lower = trimmed.toLowerCase();
        return !lower.contains("your-google")
                && !lower.contains("placeholder")
                && !lower.contains("disabled");
    }
}
