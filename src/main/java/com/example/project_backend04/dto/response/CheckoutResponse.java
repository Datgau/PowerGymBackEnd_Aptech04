package com.example.project_backend04.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutResponse {
    private Boolean success;
    private String message;
    private String paymentOrderId;
    private BigDecimal originalAmount;
    private BigDecimal promotionDiscount;
    private BigDecimal rewardDiscount;
    private Long finalAmount; // in VND
    private Integer pointsEarned;
    private Integer newTotalPoints;
    private String paymentUrl;
    private String qrCodeUrl;
}
