package com.example.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("GoogleFormsApiService - cơ chế retry khi gọi Google API")
class GoogleFormsApiServiceTest {

    @Test
    @DisplayName("Chỉ retry với lỗi tạm thời của Google (429, 5xx được liệt kê)")
    void retriesOnlyTransientGoogleFailures() {
        assertTrue(GoogleFormsApiService.isRetryable(429));
        assertTrue(GoogleFormsApiService.isRetryable(500));
        assertTrue(GoogleFormsApiService.isRetryable(502));
        assertTrue(GoogleFormsApiService.isRetryable(503));
        assertTrue(GoogleFormsApiService.isRetryable(504));
    }

    @Test
    @DisplayName("Không retry với lỗi phía client (4xx) và các mã không tạm thời")
    void doesNotRetryClientErrors() {
        assertFalse(GoogleFormsApiService.isRetryable(400));
        assertFalse(GoogleFormsApiService.isRetryable(401));
        assertFalse(GoogleFormsApiService.isRetryable(403));
        assertFalse(GoogleFormsApiService.isRetryable(404));
        assertFalse(GoogleFormsApiService.isRetryable(408));
        assertFalse(GoogleFormsApiService.isRetryable(409));
    }

    @Test
    @DisplayName("Không retry với thành công (2xx) và mã 5xx không nằm trong danh sách")
    void doesNotRetrySuccessOrUnlistedServerErrors() {
        assertFalse(GoogleFormsApiService.isRetryable(200));
        assertFalse(GoogleFormsApiService.isRetryable(201));
        assertFalse(GoogleFormsApiService.isRetryable(501));
        assertFalse(GoogleFormsApiService.isRetryable(505));
    }
}
