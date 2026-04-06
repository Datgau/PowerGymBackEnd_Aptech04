package com.example.project_backend04.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotion_usages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionUsage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "promotion_id", nullable = false)
    private Long promotionId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "payment_order_id")
    private String paymentOrderId;
    
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;
    
    @Column(name = "used_at", nullable = false, updatable = false)
    private LocalDateTime usedAt;
    
    @PrePersist
    protected void onCreate() {
        usedAt = LocalDateTime.now();
    }
}
