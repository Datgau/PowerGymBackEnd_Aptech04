package com.example.project_backend04.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {
    private String itemType; // MEMBERSHIP, SERVICE, TRAINER_BOOKING
    private String itemId;
    private String itemName;
    private BigDecimal originalAmount;
    private String promotionCode;
    private Integer rewardPointsToUse;
}
