package com.example.project_backend04.mapper;

import com.example.project_backend04.dto.response.PromotionResponse;
import com.example.project_backend04.entity.Promotion;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class PromotionMapper {
    
    public PromotionResponse toResponse(Promotion promotion) {
        if (promotion == null) {
            return null;
        }
        
        return PromotionResponse.builder()
                .id(promotion.getId())
                .code(promotion.getCode())
                .title(promotion.getTitle())
                .subtitle(promotion.getSubtitle())
                .description(promotion.getDescription())
                .image(promotion.getImage())
                .backgroundImage(promotion.getBackgroundImage())
                .type(promotion.getType())
                .discountPercentage(promotion.getDiscountPercentage())
                .discountAmount(promotion.getDiscountAmount())
                .maxDiscountAmount(promotion.getMaxDiscountAmount())
                .minPurchaseAmount(promotion.getMinPurchaseAmount())
                .validFrom(promotion.getValidFrom())
                .validUntil(promotion.getValidUntil())
                .features(promotion.getFeatures() != null ? new ArrayList<>(promotion.getFeatures()) : new ArrayList<>())
                .ctaText(promotion.getCtaText())
                .ctaLink(promotion.getCtaLink())
                .isActive(promotion.getIsActive())
                .isFeatured(promotion.getIsFeatured())
                .usageLimit(promotion.getUsageLimit())
                .usageCount(promotion.getUsageCount())
                .createdAt(promotion.getCreatedAt())
                .updatedAt(promotion.getUpdatedAt())
                .build();
    }
}
