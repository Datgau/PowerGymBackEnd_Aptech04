package com.example.project_backend04.dto.response;

import com.example.project_backend04.enums.PromotionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionResponse {
    private Long id;
    private String code;
    private String title;
    private String subtitle;
    private String description;
    private String image;
    private String backgroundImage;
    private PromotionType type;
    private BigDecimal discountPercentage;
    private BigDecimal discountAmount;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minPurchaseAmount;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private List<String> features;
    private String ctaText;
    private String ctaLink;
    private Boolean isActive;
    private Boolean isFeatured;
    private Integer usageLimit;
    private Integer usageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
