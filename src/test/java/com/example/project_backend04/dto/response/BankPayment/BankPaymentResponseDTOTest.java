package com.example.project_backend04.dto.response.BankPayment;

import com.example.project_backend04.enums.PaymentStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BankPaymentResponseDTOTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testCreateBankPaymentResponseSerialization() throws Exception {
        // Given
        LocalDateTime expiredAt = LocalDateTime.of(2023, 12, 21, 10, 45, 0);
        CreateBankPaymentResponse response = CreateBankPaymentResponse.builder()
                .qrUrl("https://img.vietqr.io/image/MB-123456789-compact2.png?amount=500000&addInfo=GYM_1_1703123456789")
                .amount(500000L)
                .content("GYM_1_1703123456789")
                .expiredAt(expiredAt)
                .orderId("ORDER_123")
                .build();

        // When
        String json = objectMapper.writeValueAsString(response);
        CreateBankPaymentResponse deserializedResponse = objectMapper.readValue(json, CreateBankPaymentResponse.class);

        // Then
        assertNotNull(json);
        assertTrue(json.contains("qrUrl"));
        assertTrue(json.contains("amount"));
        assertTrue(json.contains("content"));
        assertTrue(json.contains("expiredAt"));
        assertTrue(json.contains("orderId"));

        assertEquals(response.getQrUrl(), deserializedResponse.getQrUrl());
        assertEquals(response.getAmount(), deserializedResponse.getAmount());
        assertEquals(response.getContent(), deserializedResponse.getContent());
        assertEquals(response.getExpiredAt(), deserializedResponse.getExpiredAt());
        assertEquals(response.getOrderId(), deserializedResponse.getOrderId());
    }

    @Test
    void testPaymentStatusResponseSerialization() throws Exception {
        // Given
        LocalDateTime createdAt = LocalDateTime.of(2023, 12, 21, 10, 30, 0);
        LocalDateTime expiredAt = LocalDateTime.of(2023, 12, 21, 10, 45, 0);
        PaymentStatusResponse response = PaymentStatusResponse.builder()
                .status(PaymentStatus.PENDING)
                .orderId("ORDER_123")
                .amount(500000L)
                .createdAt(createdAt)
                .expiredAt(expiredAt)
                .itemType("SERVICE")
                .itemName("Premium Gym Membership")
                .build();

        // When
        String json = objectMapper.writeValueAsString(response);
        PaymentStatusResponse deserializedResponse = objectMapper.readValue(json, PaymentStatusResponse.class);

        // Then
        assertNotNull(json);
        assertTrue(json.contains("status"));
        assertTrue(json.contains("orderId"));
        assertTrue(json.contains("amount"));
        assertTrue(json.contains("createdAt"));
        assertTrue(json.contains("expiredAt"));
        assertTrue(json.contains("itemType"));
        assertTrue(json.contains("itemName"));

        assertEquals(response.getStatus(), deserializedResponse.getStatus());
        assertEquals(response.getOrderId(), deserializedResponse.getOrderId());
        assertEquals(response.getAmount(), deserializedResponse.getAmount());
        assertEquals(response.getCreatedAt(), deserializedResponse.getCreatedAt());
        assertEquals(response.getExpiredAt(), deserializedResponse.getExpiredAt());
        assertEquals(response.getItemType(), deserializedResponse.getItemType());
        assertEquals(response.getItemName(), deserializedResponse.getItemName());
    }

    @Test
    void testCreateBankPaymentResponseBuilder() {
        // Given
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(15);

        // When
        CreateBankPaymentResponse response = CreateBankPaymentResponse.builder()
                .qrUrl("https://example.com/qr")
                .amount(100000L)
                .content("GYM_TEST_123")
                .expiredAt(expiredAt)
                .orderId("TEST_ORDER")
                .build();

        // Then
        assertNotNull(response);
        assertEquals("https://example.com/qr", response.getQrUrl());
        assertEquals(100000L, response.getAmount());
        assertEquals("GYM_TEST_123", response.getContent());
        assertEquals(expiredAt, response.getExpiredAt());
        assertEquals("TEST_ORDER", response.getOrderId());
    }

    @Test
    void testPaymentStatusResponseBuilder() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // When
        PaymentStatusResponse response = PaymentStatusResponse.builder()
                .status(PaymentStatus.SUCCESS)
                .orderId("TEST_ORDER")
                .amount(200000L)
                .createdAt(now)
                .expiredAt(now.plusMinutes(15))
                .itemType("SERVICE")
                .itemName("Basic Membership")
                .build();

        // Then
        assertNotNull(response);
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals("TEST_ORDER", response.getOrderId());
        assertEquals(200000L, response.getAmount());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now.plusMinutes(15), response.getExpiredAt());
        assertEquals("SERVICE", response.getItemType());
        assertEquals("Basic Membership", response.getItemName());
    }
}