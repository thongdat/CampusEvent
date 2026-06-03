package com.example.controller;

import com.example.config.AcademicStructure;
import com.example.dto.LoginRequest;
import com.example.dto.LoginResponse;
import com.example.dto.RegisterRequest;
import com.example.dto.RegisterResponse;
import com.example.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @GetMapping("/test")
    public String test() {
        return "Hello World";
    }

    @GetMapping("/department-structure")
    public ResponseEntity<Map<String, Object>> departmentStructure() {
        return ResponseEntity.ok(Map.of("items", AcademicStructure.payload()));
    }

    @Autowired
    private AuthService authService;

    /**
     * POST /api/auth/login
     * UC01 – Đăng nhập hệ thống (tất cả vai trò)
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }

        String errorType = response.getErrorType();
        if ("ACCOUNT_LOCKED".equals(errorType)) {
            return ResponseEntity.status(403).body(response);
        }
        return ResponseEntity.status(401).body(response);
    }

    /**
     * POST /api/auth/register/send-otp
     * Bước 1: Gửi OTP xác minh email khi đăng ký
     * Request: { "email": "..." }
     */
    @PostMapping("/register/send-otp")
    public ResponseEntity<Map<String, Object>> sendRegistrationOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Vui lòng nhập email."
            ));
        }
        Map<String, Object> result = authService.sendRegistrationOtp(email.trim());
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/auth/register
     * Bước 2: Đăng ký tài khoản mới (phải có OTP xác minh)
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);

        if (response.isSuccess()) {
            return ResponseEntity.status(201).body(response);
        } else {
            return ResponseEntity.status(400).body(response);
        }
    }

    // ========================================
    // FORGOT PASSWORD - OTP FLOW
    // ========================================

    /**
     * POST /api/auth/forgot-password
     * Gửi mã OTP tới email
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Vui lòng nhập email."
            ));
        }
        Map<String, Object> result = authService.forgotPassword(email.trim());
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/auth/verify-otp
     * Xác minh mã OTP
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        if (email == null || otp == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Vui lòng nhập email và mã OTP."
            ));
        }
        Map<String, Object> result = authService.verifyOtp(email.trim(), otp.trim());
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/auth/reset-password
     * Đặt lại mật khẩu mới
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        String newPassword = request.get("newPassword");
        if (email == null || otp == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Vui lòng nhập đầy đủ thông tin."
            ));
        }
        if (newPassword.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Mật khẩu mới phải có ít nhất 8 ký tự."
            ));
        }
        Map<String, Object> result = authService.resetPassword(email.trim(), otp.trim(), newPassword);
        return ResponseEntity.ok(result);
    }

}
