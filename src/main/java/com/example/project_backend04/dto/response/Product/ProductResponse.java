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
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private Integer stock;
    private Integer lowStockThreshold;
    private boolean lowStock;
    private boolean outOfStock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
