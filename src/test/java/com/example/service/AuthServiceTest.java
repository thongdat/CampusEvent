package com.example.service;

import com.example.dto.RegisterRequest;
import com.example.dto.RegisterResponse;
import com.example.model.User;
import com.example.repository.UserRepository;
import com.example.security.AttemptLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test module XÁC THỰC (Anh - AnhNVT): đăng ký tài khoản, quên mật khẩu.
 *
 * Kỹ thuật: kiểm thử các nhánh validate (white-box) + dùng MOCK cho repository.
 * Tập trung vào các luật nghiệp vụ dễ kiểm chứng, không cần database thật.
 */
@DisplayName("Xác thực - Đăng ký & Quên mật khẩu (Anh)")
class AuthServiceTest {

    private AuthService authService;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
        userRepository = mock(UserRepository.class);
        // Các field dùng @Autowired nên gán mock qua reflection.
        ReflectionTestUtils.setField(authService, "userRepository", userRepository);
        ReflectionTestUtils.setField(authService, "attemptLimiter", new AttemptLimiter());
    }

    private RegisterRequest baseRequest() {
        RegisterRequest r = new RegisterRequest();
        r.setFullName("Nguyen Van A");
        r.setEmail("newuser@uni.edu.vn");
        r.setPassword("password123");
        r.setConfirmPassword("password123");
        r.setPhone("0123456789");
        r.setRole("STUDENT");
        r.setMajor("Công nghệ Thông tin");
        r.setStudentCode("HE123456");
        r.setSemester(5);
        r.setOtpCode("123456");
        return r;
    }

    @Test
    @DisplayName("Đăng ký: mật khẩu dưới 8 ký tự -> báo lỗi")
    void registerShortPassword() {
        RegisterRequest r = baseRequest();
        r.setPassword("123");
        r.setConfirmPassword("123");

        RegisterResponse res = authService.register(r);

        assertFalse(res.isSuccess());
        assertEquals("Mật khẩu phải có ít nhất 8 ký tự.", res.getMessage());
    }

    @Test
    @DisplayName("Đăng ký: mật khẩu xác nhận không khớp -> báo lỗi")
    void registerPasswordMismatch() {
        RegisterRequest r = baseRequest();
        r.setConfirmPassword("khac-mat-khau");

        RegisterResponse res = authService.register(r);

        assertFalse(res.isSuccess());
        assertEquals("Mật khẩu xác nhận không khớp.", res.getMessage());
    }

    @Test
    @DisplayName("Đăng ký: vai trò không hợp lệ (ADMIN) -> báo lỗi")
    void registerInvalidRole() {
        RegisterRequest r = baseRequest();
        r.setRole("ADMIN");

        RegisterResponse res = authService.register(r);

        assertFalse(res.isSuccess());
        assertEquals("Vai trò không hợp lệ. Chỉ được phép đăng ký với vai trò Sinh viên hoặc Khoa/Bộ môn.",
                res.getMessage());
    }

    @Test
    @DisplayName("Đăng ký: email đã tồn tại -> báo lỗi")
    void registerEmailAlreadyUsed() {
        RegisterRequest r = baseRequest();
        when(userRepository.findByEmail("newuser@uni.edu.vn")).thenReturn(Optional.of(new User()));

        RegisterResponse res = authService.register(r);

        assertFalse(res.isSuccess());
        assertEquals("Email đã được sử dụng. Vui lòng chọn email khác.", res.getMessage());
    }

    @Test
    @DisplayName("Đăng ký: số điện thoại sai định dạng -> báo lỗi")
    void registerInvalidPhone() {
        RegisterRequest r = baseRequest();
        r.setPhone("123"); // không đủ 10 chữ số
        when(userRepository.findByEmail("newuser@uni.edu.vn")).thenReturn(Optional.empty());

        RegisterResponse res = authService.register(r);

        assertFalse(res.isSuccess());
        assertEquals("Số điện thoại phải gồm đúng 10 chữ số.", res.getMessage());
    }

    @Test
    @DisplayName("Quên mật khẩu: email không tồn tại -> báo lỗi")
    void forgotPasswordUnknownEmail() {
        when(userRepository.findByEmail("khongton@uni.edu.vn")).thenReturn(Optional.empty());

        Map<String, Object> res = authService.forgotPassword("khongton@uni.edu.vn");

        assertEquals(false, res.get("success"));
        assertEquals("Email không tồn tại trong hệ thống.", res.get("message"));
    }

    @Test
    @DisplayName("Xác minh OTP: email không tồn tại -> báo lỗi")
    void verifyOtpUnknownEmail() {
        when(userRepository.findByEmail("khongton@uni.edu.vn")).thenReturn(Optional.empty());

        Map<String, Object> res = authService.verifyOtp("khongton@uni.edu.vn", "123456");

        assertEquals(false, res.get("success"));
        assertEquals("Email không tồn tại.", res.get("message"));
    }
}
