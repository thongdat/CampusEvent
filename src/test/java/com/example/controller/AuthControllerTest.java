package com.example.controller;

import com.example.security.SessionAuth;
import com.example.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    @Test
    void forgotPasswordRejectsEmailDifferentFromSignedInAccount() {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController();
        ReflectionTestUtils.setField(controller, "authService", authService);
        MockHttpServletRequest request = signedInRequest("student01@uni.edu.vn");

        ResponseEntity<Map<String, Object>> response = controller.forgotPassword(
                Map.of("email", "student02@uni.edu.vn"), request);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals(false, response.getBody().get("success"));
        assertEquals("Email không khớp với tài khoản đang đăng nhập.", response.getBody().get("message"));
        verifyNoInteractions(authService);
    }

    @Test
    void forgotPasswordAcceptsSignedInEmailIgnoringCase() {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController();
        ReflectionTestUtils.setField(controller, "authService", authService);
        MockHttpServletRequest request = signedInRequest("student01@uni.edu.vn");
        when(authService.forgotPassword("student01@uni.edu.vn"))
                .thenReturn(Map.of("success", true, "message", "Đã gửi OTP"));

        ResponseEntity<Map<String, Object>> response = controller.forgotPassword(
                Map.of("email", "STUDENT01@uni.edu.vn"), request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(true, response.getBody().get("success"));
        verify(authService).forgotPassword("student01@uni.edu.vn");
    }

    private MockHttpServletRequest signedInRequest(String email) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        SessionAuth.set(request, 1L, email, "STUDENT");
        return request;
    }
}
