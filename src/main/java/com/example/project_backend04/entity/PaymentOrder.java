package com.example.project_backend04.entity;

import com.example.project_backend04.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrder {

    @Id
    private String id;

    private Long amount;

    private String content;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime expiredAt;

    private Integer retryCount = 0;

    private String transactionRef;

    private String momoTransId;
    private String paymentMethod;
    private String paymentUrl;
    private String qrCodeUrl;
    private String deeplink;
    private String requestId;
    private String extraData;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    private String itemType;
    private String itemId;
    private String itemName;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.expiredAt = createdAt.plusMinutes(15);
    }
}