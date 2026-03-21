package com.example.project_backend04.dto.response.Payment;

import com.example.project_backend04.dto.response.User.UserShortResponse;
import com.example.project_backend04.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderResponse {
    private String id;
    private Long amount;
    private String content;
    private PaymentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiredAt;
    private Integer retryCount;
    private String transactionRef;
    private String momoTransId;
    private String paymentMethod;
    private String paymentUrl;
    private String qrCodeUrl;
    private String deeplink;
    private String requestId;
    private String extraData;
    private String itemType;
    private String itemId;
    private String itemName;
    private UserShortResponse user;
}