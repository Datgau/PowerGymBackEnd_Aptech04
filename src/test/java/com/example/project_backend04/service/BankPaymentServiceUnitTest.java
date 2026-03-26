package com.example.project_backend04.service;

import com.example.project_backend04.config.BankPaymentConfig;
import com.example.project_backend04.dto.response.BankPayment.PaymentStatusResponse;
import com.example.project_backend04.entity.PaymentOrder;
import com.example.project_backend04.enums.PaymentStatus;
import com.example.project_backend04.repository.GymServiceRepository;
import com.example.project_backend04.repository.PaymentOrderRepository;
import com.example.project_backend04.repository.ServiceRegistrationRepository;
import com.example.project_backend04.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankPaymentServiceUnitTest {

    @Mock
    private PaymentOrderRepository paymentOrderRepository;

    @Mock
    private GymServiceRepository serviceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BankPaymentConfig bankPaymentConfig;

    @Mock
    private WebhookValidationService webhookValidationService;

    @Mock
    private ServiceRegistrationRepository serviceRegistrationRepository;

    @InjectMocks
    private BankPaymentService bankPaymentService;

    private PaymentOrder mockPaymentOrder;

    @BeforeEach
    void setUp() {
        mockPaymentOrder = new PaymentOrder();
        mockPaymentOrder.setId("test-order-id");
        mockPaymentOrder.setContent("GYM_1_1703123456789");
        mockPaymentOrder.setAmount(500000L);
        mockPaymentOrder.setStatus(PaymentStatus.PENDING);
        mockPaymentOrder.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        mockPaymentOrder.setExpiredAt(LocalDateTime.now().plusMinutes(10));
        mockPaymentOrder.setItemType("SERVICE");
        mockPaymentOrder.setItemName("Premium Membership");
    }

    @Test
    void checkPaymentStatus_ValidContent_ReturnsPaymentStatusResponse() {
        // Arrange
        String content = "GYM_1_1703123456789";
        when(paymentOrderRepository.findByContent(content)).thenReturn(Optional.of(mockPaymentOrder));

        // Act
        PaymentStatusResponse result = bankPaymentService.checkPaymentStatus(content);

        // Assert
        assertNotNull(result);
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        assertEquals("test-order-id", result.getOrderId());
        assertEquals(500000L, result.getAmount());
        assertEquals("SERVICE", result.getItemType());
        assertEquals("Premium Membership", result.getItemName());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getExpiredAt());
    }

    @Test
    void checkPaymentStatus_OrderNotFound_ThrowsRuntimeException() {
        // Arrange
        String content = "INVALID_CONTENT";
        when(paymentOrderRepository.findByContent(content)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> bankPaymentService.checkPaymentStatus(content));
        assertEquals("Payment order not found with content: INVALID_CONTENT", exception.getMessage());
    }

    @Test
    void checkPaymentStatus_PaidOrder_ReturnsSuccessStatus() {
        // Arrange
        String content = "GYM_1_1703123456789";
        mockPaymentOrder.setStatus(PaymentStatus.SUCCESS);
        when(paymentOrderRepository.findByContent(content)).thenReturn(Optional.of(mockPaymentOrder));

        // Act
        PaymentStatusResponse result = bankPaymentService.checkPaymentStatus(content);

        // Assert
        assertNotNull(result);
        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        assertEquals("test-order-id", result.getOrderId());
    }

    @Test
    void checkPaymentStatus_FailedOrder_ReturnsFailedStatus() {
        // Arrange
        String content = "GYM_1_1703123456789";
        mockPaymentOrder.setStatus(PaymentStatus.FAILED);
        when(paymentOrderRepository.findByContent(content)).thenReturn(Optional.of(mockPaymentOrder));

        // Act
        PaymentStatusResponse result = bankPaymentService.checkPaymentStatus(content);

        // Assert
        assertNotNull(result);
        assertEquals(PaymentStatus.FAILED, result.getStatus());
        assertEquals("test-order-id", result.getOrderId());
    }
}