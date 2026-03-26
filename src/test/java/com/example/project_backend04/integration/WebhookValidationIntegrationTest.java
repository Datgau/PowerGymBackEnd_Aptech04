package com.example.project_backend04.integration;

import com.example.project_backend04.dto.request.BankPayment.SepayWebhookRequest;
import com.example.project_backend04.service.WebhookValidationService;
import com.example.project_backend04.exception.BankPaymentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for webhook validation functionality
 * Requirements: 6.6
 */
@SpringBootTest
class WebhookValidationIntegrationTest {

    @Autowired
    private WebhookValidationService webhookValidationService;

    @Test
    @DisplayName("Should validate webhook payload successfully in Spring context")
    void shouldValidateWebhookPayloadInSpringContext() {
        // Given
        SepayWebhookRequest validPayload = new SepayWebhookRequest(
            "GYM_123_1703123456789", 500000, "SP_TXN_123456", LocalDateTime.now().minusMinutes(1), "in");

        // When & Then
        assertDoesNotThrow(() -> webhookValidationService.validateWebhookPayload(validPayload));
    }

    @Test
    @DisplayName("Should reject malicious payload in Spring context")
    void shouldRejectMaliciousPayloadInSpringContext() {
        // Given
        SepayWebhookRequest maliciousPayload = new SepayWebhookRequest(
            "GYM_123_<script>alert('xss')</script>", 500000, "SP_TXN_123456", LocalDateTime.now(), "in");

        // When & Then
        BankPaymentException exception = assertThrows(BankPaymentException.class, 
            () -> webhookValidationService.validateWebhookPayload(maliciousPayload));
        
        assertEquals("SECURITY_VIOLATION", exception.getErrorCode());
    }

    @Test
    @DisplayName("Should sanitize input fields correctly")
    void shouldSanitizeInputFields() {
        // Given
        SepayWebhookRequest payload = new SepayWebhookRequest(
            "  GYM_123_1703123456789  ", 500000, "  SP_TXN_123456  ", LocalDateTime.now(), "in");

        // When
        webhookValidationService.validateWebhookPayload(payload);

        // Then
        assertEquals("GYM_123_1703123456789", payload.getDescription());
        assertEquals("SP_TXN_123456", payload.getTransactionId());
    }
}