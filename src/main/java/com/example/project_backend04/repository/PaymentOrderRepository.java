package com.example.project_backend04.repository;

import com.example.project_backend04.entity.PaymentOrder;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, String> {
    
    List<PaymentOrder> findByUser(User user);
    
    List<PaymentOrder> findByUserAndStatus(User user, PaymentStatus status);
    
    List<PaymentOrder> findByStatus(PaymentStatus status);
    
    List<PaymentOrder> findByStatusAndExpiredAtBefore(PaymentStatus status, LocalDateTime expiredAt);
    
    Optional<PaymentOrder> findByRequestId(String requestId);
    
    Optional<PaymentOrder> findByMomoTransId(String momoTransId);
    
    @Query("SELECT p FROM PaymentOrder p WHERE p.user = :user ORDER BY p.createdAt DESC")
    List<PaymentOrder> findByUserOrderByCreatedAtDesc(@Param("user") User user);
    
    @Query("SELECT COUNT(p) FROM PaymentOrder p WHERE p.status = :status")
    Long countByStatus(@Param("status") PaymentStatus status);
    
    @Query("SELECT SUM(p.amount) FROM PaymentOrder p WHERE p.status = :status")
    Long sumAmountByStatus(@Param("status") PaymentStatus status);
}