package com.example.project_backend04.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplyPromotionRequest {
    private String promotionCode;
    private BigDecimal orderAmount;
}
