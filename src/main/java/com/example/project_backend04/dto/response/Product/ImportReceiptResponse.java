package com.example.project_backend04.dto.response.Product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportReceiptResponse {
    private Long id;
    private String supplierName;
    private BigDecimal totalCost;
    private String notes;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private int itemCount;
}
