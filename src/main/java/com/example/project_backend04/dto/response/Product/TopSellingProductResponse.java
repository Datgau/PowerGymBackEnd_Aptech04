package com.example.project_backend04.dto.response.Product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopSellingProductResponse {
    private Long productId;
    private String productName;
    private String productImageUrl;
    private int totalQuantitySold;
    private BigDecimal totalRevenue;
}
