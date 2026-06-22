package com.example.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleFormsApiServiceTest {

    @Test
    void retriesOnlyTransientGoogleFailures() {
        assertTrue(GoogleFormsApiService.isRetryable(429));
        assertTrue(GoogleFormsApiService.isRetryable(500));
        assertTrue(GoogleFormsApiService.isRetryable(502));
        assertTrue(GoogleFormsApiService.isRetryable(503));
        assertTrue(GoogleFormsApiService.isRetryable(504));

        assertFalse(GoogleFormsApiService.isRetryable(400));
        assertFalse(GoogleFormsApiService.isRetryable(401));
        assertFalse(GoogleFormsApiService.isRetryable(403));
        assertFalse(GoogleFormsApiService.isRetryable(404));
    }
}
