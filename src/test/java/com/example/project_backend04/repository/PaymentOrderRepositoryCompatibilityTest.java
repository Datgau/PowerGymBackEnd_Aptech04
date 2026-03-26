package com.example.project_backend04.repository;

import com.example.project_backend04.entity.PaymentOrder;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * **Validates: Requirements 8.4, 8.5**
 * 
 * Repository-level tests to ensure PaymentOrderRepository methods work correctly
 * with both MoMo and Bank payment types.
 */
@DataJpaTest
@ActiveProfiles("test")
public class PaymentOrderRepositoryCompatibilityTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    private User testUser1;
    private User testUser2;
    private PaymentOrder momoPayment;
    private PaymentOrder bankPayment;
    private PaymentOrder expiredPayment;

    @BeforeEach
    void setUp() {
        // Create test users
        testUser1 = new User();
        testUser1.setFullName("Test User 1");
        testUser1.setEmail("user1@test.com");
        testUser1.setPhoneNumber("0123456789");
        testUser1 = entityManager.persistAndFlush(testUser1);

        testUser2 = new User();
        testUser2.setFullName("Test User 2");
        testUser2.setEmail("user2@test.com");
        testUser2.setPhoneNumber("0987654321");
        testUser2 = entityManager.persistAndFlush(testUser2);

        // Create MoMo payment
        momoPayment = new PaymentOrder();
        momoPayment.setId("MOMO_" + System.currentTimeMillis());
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
        momoPayment = entityManager.persistAndFlush(momoPayment);

        // Create Bank payment
        bankPayment = new PaymentOrder();
        bankPayment.setId("BANK_" + System.currentTimeMillis());
        bankPayment.setAmount(500000L);
        bankPayment.setContent("GYM_" + testUser1.getId() + "_" + System.currentTimeMillis());
        bankPayment.setStatus(PaymentStatus.PENDING);
        bankPayment.setUser(testUser1);
        bankPayment.setPaymentMethod("VIETQR");
        bankPayment.setItemType("SERVICE");
        bankPayment.setItemId("2");
        bankPayment.setItemName("VIP Membership");
        bankPayment.setQrCodeUrl("https://img.vietqr.io/image/test.png");
        bankPayment = entityManager.persistAndFlush(bankPayment);

        // Create expired payment
        expiredPayment = new PaymentOrder();
        expiredPayment.setId("EXPIRED_" + System.currentTimeMillis());
        expiredPayment.setAmount(100000L);
        expiredPayment.setContent("Expired Payment");
        expiredPayment.setStatus(PaymentStatus.PENDING);
        expiredPayment.setUser(testUser2);
        expiredPayment.setPaymentMethod("MOMO");
        expiredPayment.setItemType("SERVICE");
        expiredPayment.setCreatedAt(LocalDateTime.now().minusHours(2));
        expiredPayment.setExpiredAt(LocalDateTime.now().minusHours(1));
        expiredPayment = entityManager.persistAndFlush(expiredPayment);

        entityManager.clear();
    }

    @Test
    void testFindByUserWorksWithBothPaymentTypes() {
        List<PaymentOrder> user1Payments = paymentOrderRepository.findByUser(testUser1);
        assertEquals(2, user1Payments.size());

        boolean hasMoMo = user1Payments.stream().anyMatch(p -> "MOMO".equals(p.getPaymentMethod()));
        boolean hasVietQR = user1Payments.stream().anyMatch(p -> "VIETQR".equals(p.getPaymentMethod()));

        assertTrue(hasMoMo, "Should find MoMo payment for user");
        assertTrue(hasVietQR, "Should find VietQR payment for user");

        List<PaymentOrder> user2Payments = paymentOrderRepository.findByUser(testUser2);
        assertEquals(1, user2Payments.size());
        assertEquals("MOMO", user2Payments.get(0).getPaymentMethod());
    }

    @Test
    void testFindByUserAndStatusWorksWithBothPaymentTypes() {
        List<PaymentOrder> pendingPayments = paymentOrderRepository.findByUserAndStatus(testUser1, PaymentStatus.PENDING);
        assertEquals(2, pendingPayments.size());

        // Update one payment to SUCCESS
        momoPayment.setStatus(PaymentStatus.SUCCESS);
        entityManager.persistAndFlush(momoPayment);
        entityManager.clear();

        List<PaymentOrder> stillPending = paymentOrderRepository.findByUserAndStatus(testUser1, PaymentStatus.PENDING);
        assertEquals(1, stillPending.size());
        assertEquals("VIETQR", stillPending.get(0).getPaymentMethod());

        List<PaymentOrder> successPayments = paymentOrderRepository.findByUserAndStatus(testUser1, PaymentStatus.SUCCESS);
        assertEquals(1, successPayments.size());
        assertEquals("MOMO", successPayments.get(0).getPaymentMethod());
    }

    @Test
    void testFindByStatusWorksWithMixedPaymentTypes() {
        List<PaymentOrder> allPending = paymentOrderRepository.findByStatus(PaymentStatus.PENDING);
        assertEquals(3, allPending.size()); // momoPayment, bankPayment, expiredPayment

        // Verify we have both payment methods in pending status
        long momoCount = allPending.stream().filter(p -> "MOMO".equals(p.getPaymentMethod())).count();
        long vietqrCount = allPending.stream().filter(p -> "VIETQR".equals(p.getPaymentMethod())).count();

        assertEquals(2, momoCount); // momoPayment and expiredPayment
        assertEquals(1, vietqrCount); // bankPayment
    }

    @Test
    void testFindByStatusAndExpiredAtBeforeWorksWithBothPaymentTypes() {
        LocalDateTime now = LocalDateTime.now();
        List<PaymentOrder> expiredPendingPayments = paymentOrderRepository
            .findByStatusAndExpiredAtBefore(PaymentStatus.PENDING, now);

        assertEquals(1, expiredPendingPayments.size());
        assertEquals(expiredPayment.getId(), expiredPendingPayments.get(0).getId());
        assertEquals("MOMO", expiredPendingPayments.get(0).getPaymentMethod());

        // Test with future time to include all pending payments
        List<PaymentOrder> allPendingPayments = paymentOrderRepository
            .findByStatusAndExpiredAtBefore(PaymentStatus.PENDING, now.plusHours(1));

        assertEquals(3, allPendingPayments.size());
    }

    @Test
    void testFindByRequestIdWorksForMoMoPayments() {
        Optional<PaymentOrder> foundPayment = paymentOrderRepository.findByRequestId("REQ_MOMO_123");
        assertTrue(foundPayment.isPresent());
        assertEquals(momoPayment.getId(), foundPayment.get().getId());
        assertEquals("MOMO", foundPayment.get().getPaymentMethod());

        // Bank payments don't use requestId, so this should return empty
        Optional<PaymentOrder> notFound = paymentOrderRepository.findByRequestId("NONEXISTENT");
        assertFalse(notFound.isPresent());
    }

    @Test
    void testFindByMomoTransIdWorksForMoMoPayments() {
        Optional<PaymentOrder> foundPayment = paymentOrderRepository.findByMomoTransId("MOMO_TRANS_123");
        assertTrue(foundPayment.isPresent());
        assertEquals(momoPayment.getId(), foundPayment.get().getId());
        assertEquals("MOMO", foundPayment.get().getPaymentMethod());

        // Bank payments don't use momoTransId
        Optional<PaymentOrder> notFound = paymentOrderRepository.findByMomoTransId("NONEXISTENT");
        assertFalse(notFound.isPresent());
    }

    @Test
    void testFindByContentWorksForBothPaymentTypes() {
        // Test MoMo payment content search
        Optional<PaymentOrder> momoFound = paymentOrderRepository.findByContent("MoMo Test Payment");
        assertTrue(momoFound.isPresent());
        assertEquals(momoPayment.getId(), momoFound.get().getId());
        assertEquals("MOMO", momoFound.get().getPaymentMethod());

        // Test Bank payment content search
        Optional<PaymentOrder> bankFound = paymentOrderRepository.findByContent(bankPayment.getContent());
        assertTrue(bankFound.isPresent());
        assertEquals(bankPayment.getId(), bankFound.get().getId());
        assertEquals("VIETQR", bankFound.get().getPaymentMethod());
    }

    @Test
    void testFindByUserOrderByCreatedAtDescWorksWithBothPaymentTypes() {
        List<PaymentOrder> orderedPayments = paymentOrderRepository.findByUserOrderByCreatedAtDesc(testUser1);
        assertEquals(2, orderedPayments.size());

        // Verify ordering (most recent first)
        PaymentOrder first = orderedPayments.get(0);
        PaymentOrder second = orderedPayments.get(1);
        
        assertTrue(first.getCreatedAt().isAfter(second.getCreatedAt()) || 
                  first.getCreatedAt().equals(second.getCreatedAt()));

        // Verify both payment methods are present
        boolean hasMoMo = orderedPayments.stream().anyMatch(p -> "MOMO".equals(p.getPaymentMethod()));
        boolean hasVietQR = orderedPayments.stream().anyMatch(p -> "VIETQR".equals(p.getPaymentMethod()));
        
        assertTrue(hasMoMo);
        assertTrue(hasVietQR);
    }

    @Test
    void testCountByStatusWorksWithMixedPaymentTypes() {
        Long pendingCount = paymentOrderRepository.countByStatus(PaymentStatus.PENDING);
        assertEquals(3L, pendingCount);

        // Update one payment to SUCCESS
        bankPayment.setStatus(PaymentStatus.SUCCESS);
        entityManager.persistAndFlush(bankPayment);
        entityManager.clear();

        Long updatedPendingCount = paymentOrderRepository.countByStatus(PaymentStatus.PENDING);
        assertEquals(2L, updatedPendingCount);

        Long successCount = paymentOrderRepository.countByStatus(PaymentStatus.SUCCESS);
        assertEquals(1L, successCount);
    }

    @Test
    void testSumAmountByStatusWorksWithMixedPaymentTypes() {
        Long totalPendingAmount = paymentOrderRepository.sumAmountByStatus(PaymentStatus.PENDING);
        assertEquals(800000L, totalPendingAmount); // 200k + 500k + 100k

        // Update bank payment to SUCCESS
        bankPayment.setStatus(PaymentStatus.SUCCESS);
        entityManager.persistAndFlush(bankPayment);
        entityManager.clear();

        Long updatedPendingAmount = paymentOrderRepository.sumAmountByStatus(PaymentStatus.PENDING);
        assertEquals(300000L, updatedPendingAmount); // 200k + 100k

        Long successAmount = paymentOrderRepository.sumAmountByStatus(PaymentStatus.SUCCESS);
        assertEquals(500000L, successAmount); // bank payment amount
    }

    @Test
    void testRepositoryMethodsHandleNullValues() {
        // Test with null user
        List<PaymentOrder> nullUserPayments = paymentOrderRepository.findByUser(null);
        assertTrue(nullUserPayments.isEmpty());

        // Test with null status
        List<PaymentOrder> nullStatusPayments = paymentOrderRepository.findByStatus(null);
        assertTrue(nullStatusPayments.isEmpty());

        // Test count with null status
        Long nullStatusCount = paymentOrderRepository.countByStatus(null);
        assertEquals(0L, nullStatusCount);

        // Test sum with null status
        Long nullStatusSum = paymentOrderRepository.sumAmountByStatus(null);
        assertNull(nullStatusSum);
    }

    @Test
    void testPaymentMethodFieldConsistencyInQueries() {
        // Verify that payment method field is correctly maintained across all repository operations
        List<PaymentOrder> allPayments = paymentOrderRepository.findAll();
        
        for (PaymentOrder payment : allPayments) {
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
    }
}