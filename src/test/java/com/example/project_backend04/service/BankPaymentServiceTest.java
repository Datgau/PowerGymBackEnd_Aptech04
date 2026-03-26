package com.example.project_backend04.service;

import com.example.project_backend04.config.BankPaymentConfig;
import com.example.project_backend04.dto.request.BankPayment.SepayWebhookRequest;
import com.example.project_backend04.dto.response.BankPayment.CreateBankPaymentResponse;
import com.example.project_backend04.entity.GymService;
import com.example.project_backend04.entity.PaymentOrder;
import com.example.project_backend04.entity.User;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class BankPaymentServiceTest {

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

    private User testUser;
    private GymService testService;
    private PaymentOrder testPaymentOrder;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setFullName("Test User");
        testUser.setEmail("test@example.com");

        testService = new GymService();
        testService.setId(1L);
        testService.setName("Premium Membership");
        testService.setPrice(BigDecimal.valueOf(500000));
        testService.setIsActive(true);

        testPaymentOrder = new PaymentOrder();
        testPaymentOrder.setId("test-order-id");
        testPaymentOrder.setAmount(500000L);
        testPaymentOrder.setStatus(PaymentStatus.PENDING);
        testPaymentOrder.setCreatedAt(LocalDateTime.now());
        testPaymentOrder.setExpiredAt(LocalDateTime.now().plusMinutes(15));

        // Setup BankPaymentConfig mock with lenient stubbing
        lenient().when(bankPaymentConfig.bankCode()).thenReturn("MB");
        lenient().when(bankPaymentConfig.accountNo()).thenReturn("123456789");
        lenient().when(bankPaymentConfig.accountName()).thenReturn("TEST ACCOUNT");
        lenient().when(bankPaymentConfig.vietqrBaseUrl()).thenReturn("https://img.vietqr.io/image");
        lenient().when(bankPaymentConfig.paymentExpiryMinutes()).thenReturn(15);
    }

    @Test
    void createBankPayment_Success() {
        // Given
        Long userId = 1L;
        Long serviceId = 1L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(testService));
        when(paymentOrderRepository.save(any(PaymentOrder.class))).thenReturn(testPaymentOrder);

        // When
        CreateBankPaymentResponse response = bankPaymentService.createBankPayment(userId, serviceId);

        // Then
        assertNotNull(response);
        assertEquals(500000L, response.getAmount());
        assertTrue(response.getContent().startsWith("GYM_1_"));
        assertTrue(response.getQrUrl().contains("MB-123456789"));
        assertTrue(response.getQrUrl().contains("amount=500000"));
        assertTrue(response.getQrUrl().contains("addInfo=GYM_1_"));
        assertTrue(response.getQrUrl().contains("accountName=TEST ACCOUNT"));
        assertNotNull(response.getOrderId());

        verify(paymentOrderRepository).save(any(PaymentOrder.class));
    }

    @Test
    void createBankPayment_UserNotFound() {
        // Given
        Long userId = 999L;
        Long serviceId = 1L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> bankPaymentService.createBankPayment(userId, serviceId));
        
        assertEquals("User not found with ID: 999", exception.getMessage());
        verify(paymentOrderRepository, never()).save(any());
    }

    @Test
    void createBankPayment_ServiceNotFound() {
        // Given
        Long userId = 1L;
        Long serviceId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> bankPaymentService.createBankPayment(userId, serviceId));
        
        assertEquals("Service not found with ID: 999", exception.getMessage());
        verify(paymentOrderRepository, never()).save(any());
    }

    @Test
    void createBankPayment_ServiceNotActive() {
        // Given
        Long userId = 1L;
        Long serviceId = 1L;
        testService.setIsActive(false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(testService));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> bankPaymentService.createBankPayment(userId, serviceId));
        
        assertEquals("Service is not active: 1", exception.getMessage());
        verify(paymentOrderRepository, never()).save(any());
    }

    @Test
    void createBankPayment_PaymentOrderFieldsCorrect() {
        // Given
        Long userId = 1L;
        Long serviceId = 1L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(testService));
        when(paymentOrderRepository.save(any(PaymentOrder.class))).thenAnswer(invocation -> {
            PaymentOrder order = invocation.getArgument(0);
            
            // Verify PaymentOrder fields are set correctly
            assertNotNull(order.getId());
            assertEquals(500000L, order.getAmount());
            assertTrue(order.getContent().startsWith("GYM_1_"));
            assertEquals(PaymentStatus.PENDING, order.getStatus());
            assertEquals("VIETQR", order.getPaymentMethod());
            assertEquals("SERVICE", order.getItemType());
            assertEquals("1", order.getItemId());
            assertEquals("Premium Membership", order.getItemName());
            assertEquals(testUser, order.getUser());
            assertNotNull(order.getQrCodeUrl());
            
            return order;
        });

        // When
        CreateBankPaymentResponse response = bankPaymentService.createBankPayment(userId, serviceId);

        // Then
        assertNotNull(response);
        verify(paymentOrderRepository).save(any(PaymentOrder.class));
    }

    @Test
    void handleSepayWebhook_Success() {
        // Given
        String content = "GYM_1_1703123456789";
        SepayWebhookRequest payload = new SepayWebhookRequest();
        payload.setDescription(content);
        payload.setAmount(500000);
        payload.setTransferType("in");
        payload.setTransactionId("SP_TXN_123456");
        payload.setTimestamp(LocalDateTime.now());

        testPaymentOrder.setContent(content);
        testPaymentOrder.setAmount(500000L);
        testPaymentOrder.setStatus(PaymentStatus.PENDING);
        testPaymentOrder.setExpiredAt(LocalDateTime.now().plusMinutes(10)); // Not expired
        testPaymentOrder.setUser(testUser); // Set user to avoid NullPointerException

        when(paymentOrderRepository.findByContent(content)).thenReturn(Optional.of(testPaymentOrder));
        when(paymentOrderRepository.save(any(PaymentOrder.class))).thenReturn(testPaymentOrder);

        // When
        bankPaymentService.handleSepayWebhook(payload);

        // Then
        verify(paymentOrderRepository).save(argThat(order -> 
            order.getStatus() == PaymentStatus.SUCCESS
        ));
    }

    @Test
    void handleSepayWebhook_OrderNotFound() {
        // Given
        String content = "GYM_1_1703123456789";
        SepayWebhookRequest payload = new SepayWebhookRequest();
        payload.setDescription(content);
        payload.setAmount(500000);
        payload.setTransferType("in");

        when(paymentOrderRepository.findByContent(content)).thenReturn(Optional.empty());

        // When
        bankPaymentService.handleSepayWebhook(payload);

        // Then
        verify(paymentOrderRepository, never()).save(any());
    }

    @Test
    void handleSepayWebhook_OrderAlreadyPaid() {
        // Given
        String content = "GYM_1_1703123456789";
        SepayWebhookRequest payload = new SepayWebhookRequest();
        payload.setDescription(content);
        payload.setAmount(500000);
        payload.setTransferType("in");

        testPaymentOrder.setContent(content);
        testPaymentOrder.setStatus(PaymentStatus.SUCCESS); // Already paid

        when(paymentOrderRepository.findByContent(content)).thenReturn(Optional.of(testPaymentOrder));

        // When
        bankPaymentService.handleSepayWebhook(payload);

        // Then
        verify(paymentOrderRepository, never()).save(any());
    }

    @Test
    void handleSepayWebhook_OrderExpired() {
        // Given
        String content = "GYM_1_1703123456789";
        SepayWebhookRequest payload = new SepayWebhookRequest();
        payload.setDescription(content);
        payload.setAmount(500000);
        payload.setTransferType("in");

        testPaymentOrder.setContent(content);
        testPaymentOrder.setStatus(PaymentStatus.PENDING);
        testPaymentOrder.setExpiredAt(LocalDateTime.now().minusMinutes(5)); // Expired

        when(paymentOrderRepository.findByContent(content)).thenReturn(Optional.of(testPaymentOrder));
        when(paymentOrderRepository.save(any(PaymentOrder.class))).thenReturn(testPaymentOrder);

        // When
        bankPaymentService.handleSepayWebhook(payload);

        // Then
        verify(paymentOrderRepository).save(argThat(order -> 
            order.getStatus() == PaymentStatus.FAILED
        ));
    }

    @Test
    void handleSepayWebhook_AmountMismatch() {
        // Given
        String content = "GYM_1_1703123456789";
        SepayWebhookRequest payload = new SepayWebhookRequest();
        payload.setDescription(content);
        payload.setAmount(300000); // Different amount
        payload.setTransferType("in");

        testPaymentOrder.setContent(content);
        testPaymentOrder.setAmount(500000L); // Expected amount
        testPaymentOrder.setStatus(PaymentStatus.PENDING);
        testPaymentOrder.setExpiredAt(LocalDateTime.now().plusMinutes(10)); // Not expired

        when(paymentOrderRepository.findByContent(content)).thenReturn(Optional.of(testPaymentOrder));

        // When
        bankPaymentService.handleSepayWebhook(payload);

        // Then
        verify(paymentOrderRepository, never()).save(any());
    }
}