package com.example.project_backend04.repository;

import com.example.project_backend04.enums.DeliveryStatus;
import com.example.project_backend04.entity.PaymentStatus;
import com.example.project_backend04.entity.ProductOrder;
import com.example.project_backend04.enums.SaleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductOrderRepository extends JpaRepository<ProductOrder, Long>,
                                                JpaSpecificationExecutor<ProductOrder> {

    List<ProductOrder> findByUserId(Long userId);

    List<ProductOrder> findByPaymentStatus(PaymentStatus status);

    List<ProductOrder> findByDeliveryStatus(DeliveryStatus status);

    List<ProductOrder> findByCustomerNameContainingIgnoreCaseOrCustomerPhoneContaining(
        String name, 
        String phone
    );

    @Query("SELECT po FROM ProductOrder po WHERE " +
           "(:paymentStatus IS NULL OR po.paymentStatus = :paymentStatus) AND " +
           "(:deliveryStatus IS NULL OR po.deliveryStatus = :deliveryStatus) AND " +
           "(:saleType IS NULL OR po.saleType = :saleType) AND " +
           "(:startDate IS NULL OR po.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR po.createdAt <= :endDate) AND " +
           "(:search IS NULL OR LOWER(po.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "po.customerPhone LIKE CONCAT('%', :search, '%')) " +
           "ORDER BY po.createdAt DESC")
    Page<ProductOrder> findByComplexFilters(
        @Param("paymentStatus") PaymentStatus paymentStatus,
        @Param("deliveryStatus") DeliveryStatus deliveryStatus,
        @Param("saleType") SaleType saleType,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("search") String search,
        Pageable pageable
    );

    @Query("""
    SELECT COALESCE(SUM(po.totalAmount), 0)
    FROM ProductOrder po
    WHERE po.paymentStatus = com.example.project_backend04.entity.PaymentStatus.PAID
    AND po.createdAt BETWEEN :startDate AND :endDate
    """)
    BigDecimal calculateTotalRevenue(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
    SELECT COUNT(po)
    FROM ProductOrder po
    WHERE po.createdAt BETWEEN :startDate AND :endDate
""")
    long countOrdersByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("""
    SELECT COUNT(po)
    FROM ProductOrder po
    WHERE po.paymentStatus = :paymentStatus
    AND po.createdAt BETWEEN :startDate AND :endDate
""")
    long countOrdersByPaymentStatus(
        @Param("paymentStatus") PaymentStatus paymentStatus,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("""
    SELECT COUNT(po)
    FROM ProductOrder po
    WHERE po.deliveryStatus = :deliveryStatus
    AND po.createdAt BETWEEN :startDate AND :endDate
""")
    long countOrdersByDeliveryStatus(
        @Param("deliveryStatus") DeliveryStatus deliveryStatus,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );



    @Query("""
    SELECT po
    FROM ProductOrder po
    WHERE po.paymentStatus = com.example.project_backend04.entity.PaymentStatus.PAID
    AND po.createdAt BETWEEN :startDate AND :endDate
""")
    List<ProductOrder> findPaidOrdersByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    boolean existsByPaymentId(String paymentId);
}
