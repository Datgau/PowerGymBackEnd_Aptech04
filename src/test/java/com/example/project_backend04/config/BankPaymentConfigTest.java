package com.example.project_backend04.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "bank.payment.bank-code=TEST_BANK",
    "bank.payment.account-no=1234567890",
    "bank.payment.account-name=Test Account",
    "bank.payment.sepay-api-key=test-api-key",
    "bank.payment.vietqr-base-url=https://test.vietqr.io/image",
    "bank.payment.payment-expiry-minutes=30",
    "bank.payment.webhook-rate-limit=200"
})
class BankPaymentConfigTest {

    @Autowired
    private BankPaymentConfig bankPaymentConfig;

    @Test
    void testConfigurationPropertiesBinding() {
        // Test that configuration properties are correctly bound
        assertEquals("TEST_BANK", bankPaymentConfig.bankCode());
        assertEquals("1234567890", bankPaymentConfig.accountNo());
        assertEquals("Test Account", bankPaymentConfig.accountName());
        assertEquals("test-api-key", bankPaymentConfig.sepayApiKey());
        assertEquals("https://test.vietqr.io/image", bankPaymentConfig.vietqrBaseUrl());
        assertEquals(30, bankPaymentConfig.paymentExpiryMinutes());
        assertEquals(200, bankPaymentConfig.webhook().rateLimitPerMinute());
    }

    @Test
    void testConfigurationNotNull() {
        // Ensure the configuration bean is properly injected
        assertNotNull(bankPaymentConfig);
    }
}