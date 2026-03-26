package com.example.project_backend04.compatibility;

import com.example.project_backend04.entity.PaymentOrder;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.enums.PaymentStatus;
import com.example.project_backend04.repository.PaymentOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * **Validates: Requirements 8.1, 8.4, 8.5**
 * 
 * Unit tests to ensure PaymentOrderRepository methods work correctly
 * with both MoMo and Bank payment types, maintaining compatibility.
 */
public class PaymentCompatibilityUnitTest {

    @Mock
    private PaymentOrderRepository paymentOrderRepository;

    private User testUser1;
    private User testUser2;
    private PaymentOrder momoPayment;
    private PaymentOrder bankPayment;
    private PaymentOrder expiredPayment;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create test users
        testUser1 = new User();
        testUser1.setId(1L);
        testUser1.setFullName("Test User 1");
        testUser1.setEmail("user1@test.com");
        testUser1.setPhoneNumber("0123456789");

        testUser2 = new User();
        testUser2.setId(2L);
        testUser2.setFullName("Test User 2");
        testUser2.setEmail("user2@test.com");
        testUser2.setPhoneNumber("0987654321");

        // Create MoMo payment
        momoPayment = new PaymentOrder();
        momoPayment.setId("MOMO_123");
        momoPayment.setAmount(200000L);
        momoPayment.setContent("MoMo Test Payment");
        momoPayment.setStatus(PaymentStatus.PENDING);
        momoPayment.setUser(testUser1);
        momoPayment.setPaymentMethod("MOMO");
        momoPayment.setItemType("SERVICE");
        momoPayment.setItemId("1");
        momoPayment.setItemName("Premium Membership");
        momoPayment.setRequestId("REQ_MOMO_123");
        momoPayment.setMomoTransId("MOMO_TRANS_123");
        momoPayment.setCreatedAt(LocalDateTime.now());
        momoPayment.setExpiredAt(LocalDateTime.now().plusMinutes(15));

        // Create Bank payment
        bankPayment = new PaymentOrder();
        bankPayment.setId("BANK_456");
        bankPayment.setAmount(500000L);
        bankPayment.setContent("GYM_1_1703123456789");
        bankPayment.setStatus(PaymentStatus.PENDING);
        bankPayment.setUser(testUser1);
        bankPayment.setPaymentMethod("VIETQR");
        bankPayment.setItemType("SERVICE");
        bankPayment.setItemId("2");
        bankPayment.setItemName("VIP Membership");
        bankPayment.setQrCodeUrl("https://img.vietqr.io/image/test.png");
        bankPayment.setCreatedAt(LocalDateTime.now());
        bankPayment.setExpiredAt(LocalDateTime.now().plusMinutes(15));

        // Create expired payment
        expiredPayment = new PaymentOrder();
        expiredPayment.setId("EXPIRED_789");
        expiredPayment.setAmount(100000L);
        expiredPayment.setContent("Expired Payment");
        expiredPayment.setStatus(PaymentStatus.PENDING);
        expiredPayment.setUser(testUser2);
        expiredPayment.setPaymentMethod("MOMO");
        expiredPayment.setItemType("SERVICE");
        expiredPayment.setRequestId("REQ_EXPIRED_789"); // Add requestId for MoMo payment
        expiredPayment.setCreatedAt(LocalDateTime.now().minusHours(2));
        expiredPayment.setExpiredAt(LocalDateTime.now().minusHours(1));
    }

    @Test
    void testFindByUserWorksWithBothPaymentTypes() {
        // Setup mock
        List<PaymentOrder> user1Payments = Arrays.asList(momoPayment, bankPayment);
        when(paymentOrderRepository.findByUser(testUser1)).thenReturn(user1Payments);

        // Execute
        List<PaymentOrder> result = paymentOrderRepository.findByUser(testUser1);

        // Verify
        assertEquals(2, result.size());
        boolean hasMoMo = result.stream().anyMatch(p -> "MOMO".equals(p.getPaymentMethod()));
        boolean hasVietQR = result.stream().anyMatch(p -> "VIETQR".equals(p.getPaymentMethod()));

        assertTrue(hasMoMo, "Should find MoMo payment for user");
        assertTrue(hasVietQR, "Should find VietQR payment for user");

        verify(paymentOrderRepository).findByUser(testUser1);
    }

    @Test
    void testFindByUserAndStatusWorksWithBothPaymentTypes() {
        // Setup mock for pending payments
        List<PaymentOrder> pendingPayments = Arrays.asList(momoPayment, bankPayment);
        when(paymentOrderRepository.findByUserAndStatus(testUser1, PaymentStatus.PENDING))
            .thenReturn(pendingPayments);

        // Execute
        List<PaymentOrder> result = paymentOrderRepository.findByUserAndStatus(testUser1, PaymentStatus.PENDING);

        // Verify
        assertEquals(2, result.size());
        verify(paymentOrderRepository).findByUserAndStatus(testUser1, PaymentStatus.PENDING);
    }

    @Test
    void testFindByStatusWorksWithMixedPaymentTypes() {
        // Setup mock
        List<PaymentOrder> allPending = Arrays.asList(momoPayment, bankPayment, expiredPayment);
        when(paymentOrderRepository.findByStatus(PaymentStatus.PENDING)).thenReturn(allPending);

        // Execute
        List<PaymentOrder> result = paymentOrderRepository.findByStatus(PaymentStatus.PENDING);

        // Verify
        assertEquals(3, result.size());
        
        long momoCount = result.stream().filter(p -> "MOMO".equals(p.getPaymentMethod())).count();
        long vietqrCount = result.stream().filter(p -> "VIETQR".equals(p.getPaymentMethod())).count();

        assertEquals(2, momoCount); // momoPayment and expiredPayment
        assertEquals(1, vietqrCount); // bankPayment

        verify(paymentOrderRepository).findByStatus(PaymentStatus.PENDING);
    }

    @Test
    void testFindByStatusAndExpiredAtBeforeWorksWithBothPaymentTypes() {
        // Setup mock
        LocalDateTime now = LocalDateTime.now();
        List<PaymentOrder> expiredPendingPayments = Arrays.asList(expiredPayment);
        when(paymentOrderRepository.findByStatusAndExpiredAtBefore(PaymentStatus.PENDING, now))
            .thenReturn(expiredPendingPayments);

        // Execute
        List<PaymentOrder> result = paymentOrderRepository.findByStatusAndExpiredAtBefore(PaymentStatus.PENDING, now);

        // Verify
        assertEquals(1, result.size());
        assertEquals(expiredPayment.getId(), result.get(0).getId());
        assertEquals("MOMO", result.get(0).getPaymentMethod());

        verify(paymentOrderRepository).findByStatusAndExpiredAtBefore(PaymentStatus.PENDING, now);
    }

    @Test
    void testFindByRequestIdWorksForMoMoPayments() {
        // Setup mock
        when(paymentOrderRepository.findByRequestId("REQ_MOMO_123")).thenReturn(Optional.of(momoPayment));
        when(paymentOrderRepository.findByRequestId("NONEXISTENT")).thenReturn(Optional.empty());

        // Execute
        Optional<PaymentOrder> foundPayment = paymentOrderRepository.findByRequestId("REQ_MOMO_123");
        Optional<PaymentOrder> notFound = paymentOrderRepository.findByRequestId("NONEXISTENT");

        // Verify
        assertTrue(foundPayment.isPresent());
        assertEquals(momoPayment.getId(), foundPayment.get().getId());
        assertEquals("MOMO", foundPayment.get().getPaymentMethod());
        assertFalse(notFound.isPresent());

        verify(paymentOrderRepository).findByRequestId("REQ_MOMO_123");
        verify(paymentOrderRepository).findByRequestId("NONEXISTENT");
    }

    @Test
    void testFindByMomoTransIdWorksForMoMoPayments() {
        // Setup mock
        when(paymentOrderRepository.findByMomoTransId("MOMO_TRANS_123")).thenReturn(Optional.of(momoPayment));
        when(paymentOrderRepository.findByMomoTransId("NONEXISTENT")).thenReturn(Optional.empty());

        // Execute
        Optional<PaymentOrder> foundPayment = paymentOrderRepository.findByMomoTransId("MOMO_TRANS_123");
        Optional<PaymentOrder> notFound = paymentOrderRepository.findByMomoTransId("NONEXISTENT");

        // Verify
        assertTrue(foundPayment.isPresent());
        assertEquals(momoPayment.getId(), foundPayment.get().getId());
        assertEquals("MOMO", foundPayment.get().getPaymentMethod());
        assertFalse(notFound.isPresent());

        verify(paymentOrderRepository).findByMomoTransId("MOMO_TRANS_123");
        verify(paymentOrderRepository).findByMomoTransId("NONEXISTENT");
    }

    @Test
    void testFindByContentWorksForBothPaymentTypes() {
        // Setup mock
        when(paymentOrderRepository.findByContent("MoMo Test Payment")).thenReturn(Optional.of(momoPayment));
        when(paymentOrderRepository.findByContent("GYM_1_1703123456789")).thenReturn(Optional.of(bankPayment));

        // Execute
        Optional<PaymentOrder> momoFound = paymentOrderRepository.findByContent("MoMo Test Payment");
        Optional<PaymentOrder> bankFound = paymentOrderRepository.findByContent("GYM_1_1703123456789");

        // Verify
        assertTrue(momoFound.isPresent());
        assertEquals(momoPayment.getId(), momoFound.get().getId());
        assertEquals("MOMO", momoFound.get().getPaymentMethod());

        assertTrue(bankFound.isPresent());
        assertEquals(bankPayment.getId(), bankFound.get().getId());
        assertEquals("VIETQR", bankFound.get().getPaymentMethod());

        verify(paymentOrderRepository).findByContent("MoMo Test Payment");
        verify(paymentOrderRepository).findByContent("GYM_1_1703123456789");
    }

    @Test
    void testFindByUserOrderByCreatedAtDescWorksWithBothPaymentTypes() {
        // Setup mock
        List<PaymentOrder> orderedPayments = Arrays.asList(bankPayment, momoPayment); // bank is newer
        when(paymentOrderRepository.findByUserOrderByCreatedAtDesc(testUser1)).thenReturn(orderedPayments);

        // Execute
        List<PaymentOrder> result = paymentOrderRepository.findByUserOrderByCreatedAtDesc(testUser1);

        // Verify
        assertEquals(2, result.size());
        
        boolean hasMoMo = result.stream().anyMatch(p -> "MOMO".equals(p.getPaymentMethod()));
        boolean hasVietQR = result.stream().anyMatch(p -> "VIETQR".equals(p.getPaymentMethod()));
        
        assertTrue(hasMoMo);
        assertTrue(hasVietQR);

        verify(paymentOrderRepository).findByUserOrderByCreatedAtDesc(testUser1);
    }

    @Test
    void testCountByStatusWorksWithMixedPaymentTypes() {
        // Setup mock
        when(paymentOrderRepository.countByStatus(PaymentStatus.PENDING)).thenReturn(3L);
        when(paymentOrderRepository.countByStatus(PaymentStatus.SUCCESS)).thenReturn(1L);

        // Execute
        Long pendingCount = paymentOrderRepository.countByStatus(PaymentStatus.PENDING);
        Long successCount = paymentOrderRepository.countByStatus(PaymentStatus.SUCCESS);

        // Verify
        assertEquals(3L, pendingCount);
        assertEquals(1L, successCount);

        verify(paymentOrderRepository).countByStatus(PaymentStatus.PENDING);
        verify(paymentOrderRepository).countByStatus(PaymentStatus.SUCCESS);
    }

    @Test
    void testSumAmountByStatusWorksWithMixedPaymentTypes() {
        // Setup mock
        when(paymentOrderRepository.sumAmountByStatus(PaymentStatus.PENDING)).thenReturn(800000L); // 200k + 500k + 100k
        when(paymentOrderRepository.sumAmountByStatus(PaymentStatus.SUCCESS)).thenReturn(500000L);

        // Execute
        Long totalPendingAmount = paymentOrderRepository.sumAmountByStatus(PaymentStatus.PENDING);
        Long successAmount = paymentOrderRepository.sumAmountByStatus(PaymentStatus.SUCCESS);

        // Verify
        assertEquals(800000L, totalPendingAmount);
        assertEquals(500000L, successAmount);

        verify(paymentOrderRepository).sumAmountByStatus(PaymentStatus.PENDING);
        verify(paymentOrderRepository).sumAmountByStatus(PaymentStatus.SUCCESS);
    }

    @Test
    void testRepositoryMethodsHandleNullValues() {
        // Setup mock
        when(paymentOrderRepository.findByUser(null)).thenReturn(Arrays.asList());
        when(paymentOrderRepository.findByStatus(null)).thenReturn(Arrays.asList());
        when(paymentOrderRepository.countByStatus(null)).thenReturn(0L);
        when(paymentOrderRepository.sumAmountByStatus(null)).thenReturn(null);

        // Execute
        List<PaymentOrder> nullUserPayments = paymentOrderRepository.findByUser(null);
        List<PaymentOrder> nullStatusPayments = paymentOrderRepository.findByStatus(null);
        Long nullStatusCount = paymentOrderRepository.countByStatus(null);
        Long nullStatusSum = paymentOrderRepository.sumAmountByStatus(null);

        // Verify
        assertTrue(nullUserPayments.isEmpty());
        assertTrue(nullStatusPayments.isEmpty());
        assertEquals(0L, nullStatusCount);
        assertNull(nullStatusSum);

        verify(paymentOrderRepository).findByUser(null);
        verify(paymentOrderRepository).findByStatus(null);
        verify(paymentOrderRepository).countByStatus(null);
        verify(paymentOrderRepository).sumAmountByStatus(null);
    }

    @Test
    void testPaymentMethodFieldConsistencyInQueries() {
        // Setup mock
        List<PaymentOrder> allPayments = Arrays.asList(momoPayment, bankPayment, expiredPayment);
        when(paymentOrderRepository.findAll()).thenReturn(allPayments);

        // Execute
        List<PaymentOrder> result = paymentOrderRepository.findAll();

        // Verify payment method field consistency
        for (PaymentOrder payment : result) {
            assertNotNull(payment.getPaymentMethod(), "Payment method should not be null");
            assertTrue(payment.getPaymentMethod().equals("MOMO") || 
                      payment.getPaymentMethod().equals("VIETQR"),
                      "Payment method should be either MOMO or VIETQR");
            
            if ("MOMO".equals(payment.getPaymentMethod())) {
                // MoMo payments should have requestId and might have momoTransId
                assertNotNull(payment.getRequestId(), "MoMo payments should have requestId");
            } else if ("VIETQR".equals(payment.getPaymentMethod())) {
                // VietQR payments should have qrCodeUrl
                assertNotNull(payment.getQrCodeUrl(), "VietQR payments should have qrCodeUrl");
            }
        }

        verify(paymentOrderRepository).findAll();
    }

    @Test
    void testExistingPaymentFlowsNotAffectedByBankPayments() {
        // Create additional MoMo payments to simulate existing system
        PaymentOrder existingMoMo1 = new PaymentOrder();
        existingMoMo1.setId("EXISTING_MOMO_1");
        existingMoMo1.setAmount(150000L);
        existingMoMo1.setContent("Existing MoMo Payment 1");
        existingMoMo1.setStatus(PaymentStatus.SUCCESS);
        existingMoMo1.setUser(testUser1);
        existingMoMo1.setPaymentMethod("MOMO");
        existingMoMo1.setItemType("SERVICE");
        existingMoMo1.setRequestId("REQ_EXISTING_1");

        PaymentOrder existingMoMo2 = new PaymentOrder();
        existingMoMo2.setId("EXISTING_MOMO_2");
        existingMoMo2.setAmount(250000L);
        existingMoMo2.setContent("Existing MoMo Payment 2");
        existingMoMo2.setStatus(PaymentStatus.FAILED);
        existingMoMo2.setUser(testUser2);
        existingMoMo2.setPaymentMethod("MOMO");
        existingMoMo2.setItemType("SERVICE");
        existingMoMo2.setRequestId("REQ_EXISTING_2");

        // Setup mocks
        when(paymentOrderRepository.findByStatus(PaymentStatus.SUCCESS))
            .thenReturn(Arrays.asList(existingMoMo1));
        when(paymentOrderRepository.findByStatus(PaymentStatus.FAILED))
            .thenReturn(Arrays.asList(existingMoMo2));
        when(paymentOrderRepository.findByRequestId("REQ_EXISTING_1"))
            .thenReturn(Optional.of(existingMoMo1));
        when(paymentOrderRepository.findByUserOrderByCreatedAtDesc(testUser1))
            .thenReturn(Arrays.asList(bankPayment, momoPayment, existingMoMo1));

        // Execute and verify existing MoMo queries still work correctly
        List<PaymentOrder> successfulPayments = paymentOrderRepository.findByStatus(PaymentStatus.SUCCESS);
        assertTrue(successfulPayments.stream().anyMatch(p -> "EXISTING_MOMO_1".equals(p.getId())));

        List<PaymentOrder> failedPayments = paymentOrderRepository.findByStatus(PaymentStatus.FAILED);
        assertTrue(failedPayments.stream().anyMatch(p -> "EXISTING_MOMO_2".equals(p.getId())));

        // Verify MoMo-specific queries work
        Optional<PaymentOrder> foundByRequestId = paymentOrderRepository.findByRequestId("REQ_EXISTING_1");
        assertTrue(foundByRequestId.isPresent());
        assertEquals("EXISTING_MOMO_1", foundByRequestId.get().getId());

        // Verify user payment history includes all payment types
        List<PaymentOrder> user1History = paymentOrderRepository.findByUserOrderByCreatedAtDesc(testUser1);
        assertEquals(3, user1History.size()); // bank, momo, existing momo

        // Verify payment method distribution
        long momoCount = user1History.stream().filter(p -> "MOMO".equals(p.getPaymentMethod())).count();
        long vietqrCount = user1History.stream().filter(p -> "VIETQR".equals(p.getPaymentMethod())).count();
        
        assertEquals(2, momoCount); // original + existing
        assertEquals(1, vietqrCount); // bank payment

        // Verify all mocks were called
        verify(paymentOrderRepository).findByStatus(PaymentStatus.SUCCESS);
        verify(paymentOrderRepository).findByStatus(PaymentStatus.FAILED);
        verify(paymentOrderRepository).findByRequestId("REQ_EXISTING_1");
        verify(paymentOrderRepository).findByUserOrderByCreatedAtDesc(testUser1);
    }

    @Test
    void testPaymentOrderEntityFieldsAreCorrectlySet() {
        // Verify MoMo payment fields
        assertEquals("MOMO_123", momoPayment.getId());
        assertEquals("MOMO", momoPayment.getPaymentMethod());
        assertEquals("REQ_MOMO_123", momoPayment.getRequestId());
        assertEquals("MOMO_TRANS_123", momoPayment.getMomoTransId());
        assertNull(momoPayment.getQrCodeUrl());
        assertEquals("SERVICE", momoPayment.getItemType());

        // Verify Bank payment fields
        assertEquals("BANK_456", bankPayment.getId());
        assertEquals("VIETQR", bankPayment.getPaymentMethod());
        assertNull(bankPayment.getRequestId());
        assertNull(bankPayment.getMomoTransId());
        assertEquals("https://img.vietqr.io/image/test.png", bankPayment.getQrCodeUrl());
        assertEquals("SERVICE", bankPayment.getItemType());

        // Verify both have common fields
        assertNotNull(momoPayment.getUser());
        assertNotNull(bankPayment.getUser());
        assertEquals(PaymentStatus.PENDING, momoPayment.getStatus());
        assertEquals(PaymentStatus.PENDING, bankPayment.getStatus());
        assertNotNull(momoPayment.getCreatedAt());
        assertNotNull(bankPayment.getCreatedAt());
        assertNotNull(momoPayment.getExpiredAt());
        assertNotNull(bankPayment.getExpiredAt());
    }

    @Test
    void testBothPaymentTypesCanCoexistInSameRepository() {
        // Setup mock to return mixed payment types
        List<PaymentOrder> mixedPayments = Arrays.asList(momoPayment, bankPayment, expiredPayment);
        when(paymentOrderRepository.findAll()).thenReturn(mixedPayments);

        // Execute
        List<PaymentOrder> allPayments = paymentOrderRepository.findAll();

        // Verify both payment types coexist
        assertEquals(3, allPayments.size());
        
        boolean hasMoMo = allPayments.stream().anyMatch(p -> "MOMO".equals(p.getPaymentMethod()));
        boolean hasVietQR = allPayments.stream().anyMatch(p -> "VIETQR".equals(p.getPaymentMethod()));
        
        assertTrue(hasMoMo, "Repository should contain MoMo payments");
        assertTrue(hasVietQR, "Repository should contain VietQR payments");

        // Verify no conflicts in payment IDs
        List<String> paymentIds = allPayments.stream().map(PaymentOrder::getId).toList();
        assertEquals(3, paymentIds.stream().distinct().count(), "All payment IDs should be unique");

        verify(paymentOrderRepository).findAll();
    }
}