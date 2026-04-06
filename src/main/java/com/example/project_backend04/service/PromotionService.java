package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.ApplyPromotionRequest;
import com.example.project_backend04.dto.response.ApplyPromotionResponse;
import com.example.project_backend04.dto.response.PromotionResponse;
import com.example.project_backend04.entity.Promotion;
import com.example.project_backend04.entity.PromotionUsage;
import com.example.project_backend04.mapper.PromotionMapper;
import com.example.project_backend04.repository.PromotionRepository;
import com.example.project_backend04.repository.PromotionUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionService {
    
    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository promotionUsageRepository;
    private final PromotionMapper promotionMapper;
    
    @Transactional(readOnly = true)
    public List<PromotionResponse> getActivePromotions() {
        return promotionRepository.findActivePromotions(LocalDateTime.now())
                .stream()
                .map(promotionMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<PromotionResponse> getFeaturedPromotions() {
        return promotionRepository.findFeaturedPromotions()
                .stream()
                .map(promotionMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public ApplyPromotionResponse validateAndCalculatePromotion(Long userId, ApplyPromotionRequest request) {
        log.info("Validating promotion: code={}, userId={}, orderAmount={}", 
            request.getPromotionCode(), userId, request.getOrderAmount());
        
        Promotion promotion = promotionRepository.findByCode(request.getPromotionCode())
            .orElse(null);
        
        if (promotion == null) {
            log.warn("Promotion not found: code={}", request.getPromotionCode());
            return ApplyPromotionResponse.builder()
                .success(false)
                .message("Mã khuyến mãi không tồn tại")
                .build();
        }
        
        LocalDateTime now = LocalDateTime.now();
        log.info("Found promotion: id={}, title={}, type={}, isActive={}, validFrom={}, validUntil={}, minPurchase={}", 
            promotion.getId(), promotion.getTitle(), promotion.getType(), 
            promotion.getIsActive(), promotion.getValidFrom(), promotion.getValidUntil(),
            promotion.getMinPurchaseAmount());
        
        // Validate promotion with detailed error messages
        if (!promotion.isValid(request.getOrderAmount())) {
            log.warn("Promotion validation failed");
            
            // Build debug info
            ApplyPromotionResponse.DebugInfo debugInfo = ApplyPromotionResponse.DebugInfo.builder()
                .isActive(promotion.getIsActive())
                .validFrom(promotion.getValidFrom())
                .validUntil(promotion.getValidUntil())
                .currentTime(now)
                .usageCount(promotion.getUsageCount())
                .usageLimit(promotion.getUsageLimit())
                .minPurchaseAmount(promotion.getMinPurchaseAmount())
                .orderAmount(request.getOrderAmount())
                .build();
            
            // Check specific failure reason
            if (!Boolean.TRUE.equals(promotion.getIsActive())) {
                debugInfo.setFailureReason("Promotion is not active");
                return ApplyPromotionResponse.builder()
                    .success(false)
                    .message("Mã khuyến mãi đã bị vô hiệu hóa")
                    .debugInfo(debugInfo)
                    .build();
            }
            if (promotion.getValidFrom() != null && now.isBefore(promotion.getValidFrom())) {
                debugInfo.setFailureReason("Current time is before validFrom");
                return ApplyPromotionResponse.builder()
                    .success(false)
                    .message("Mã khuyến mãi chưa có hiệu lực")
                    .debugInfo(debugInfo)
                    .build();
            }
            if (promotion.getValidUntil() != null && now.isAfter(promotion.getValidUntil())) {
                debugInfo.setFailureReason("Current time is after validUntil");
                return ApplyPromotionResponse.builder()
                    .success(false)
                    .message("Mã khuyến mãi đã hết hạn")
                    .debugInfo(debugInfo)
                    .build();
            }
            if (promotion.getUsageLimit() != null && promotion.getUsageCount() >= promotion.getUsageLimit()) {
                debugInfo.setFailureReason("Usage limit exceeded");
                return ApplyPromotionResponse.builder()
                    .success(false)
                    .message("Mã khuyến mãi đã hết lượt sử dụng")
                    .debugInfo(debugInfo)
                    .build();
            }
            if (promotion.getMinPurchaseAmount() != null && request.getOrderAmount().compareTo(promotion.getMinPurchaseAmount()) < 0) {
                debugInfo.setFailureReason("Order amount is less than minimum purchase amount");
                return ApplyPromotionResponse.builder()
                    .success(false)
                    .message(String.format("Đơn hàng tối thiểu phải từ %s VND để sử dụng mã này", 
                        promotion.getMinPurchaseAmount().intValue()))
                    .debugInfo(debugInfo)
                    .build();
            }
            
            debugInfo.setFailureReason("Unknown validation failure");
            return ApplyPromotionResponse.builder()
                .success(false)
                .message("Mã khuyến mãi không hợp lệ hoặc đã hết hạn")
                .debugInfo(debugInfo)
                .build();
        }
        
        // Check usage limit per user (if needed)
        long userUsageCount = promotionUsageRepository.countByPromotionIdAndUserId(promotion.getId(), userId);
        log.info("User usage count: userId={}, promotionId={}, count={}", userId, promotion.getId(), userUsageCount);
        
        if (userUsageCount > 0) {
            return ApplyPromotionResponse.builder()
                .success(false)
                .message("Bạn đã sử dụng mã khuyến mãi này rồi")
                .debugInfo(ApplyPromotionResponse.DebugInfo.builder()
                    .failureReason("User has already used this promotion")
                    .build())
                .build();
        }
        
        // Calculate discount
        BigDecimal discountAmount = promotion.calculateDiscount(request.getOrderAmount());
        BigDecimal finalAmount = request.getOrderAmount().subtract(discountAmount);
        
        log.info("Promotion applied successfully: code={}, originalAmount={}, discountAmount={}, finalAmount={}", 
            promotion.getCode(), request.getOrderAmount(), discountAmount, finalAmount);
        
        return ApplyPromotionResponse.builder()
            .success(true)
            .message("Áp dụng mã khuyến mãi thành công")
            .promotionId(promotion.getId())
            .promotionCode(promotion.getCode())
            .promotionName(promotion.getTitle())
            .originalAmount(request.getOrderAmount())
            .discountAmount(discountAmount)
            .finalAmount(finalAmount)
            .build();
    }
    
    @Transactional
    public void recordPromotionUsage(Long promotionId, Long userId, String paymentOrderId, BigDecimal discountAmount) {
        PromotionUsage usage = PromotionUsage.builder()
            .promotionId(promotionId)
            .userId(userId)
            .paymentOrderId(paymentOrderId)
            .discountAmount(discountAmount)
            .build();
        
        promotionUsageRepository.save(usage);
        
        // Increment usage count
        Promotion promotion = promotionRepository.findById(promotionId)
            .orElseThrow(() -> new RuntimeException("Promotion not found"));
        promotion.setUsageCount(promotion.getUsageCount() + 1);
        promotionRepository.save(promotion);
        
        log.info("Recorded promotion usage: promotionId={}, userId={}, discount={}", 
            promotionId, userId, discountAmount);
    }
    
    @Transactional
    public void incrementUsageCount(Long promotionId, Long userId) {
        log.info("Incrementing usage count for promotion: promotionId={}, userId={}", promotionId, userId);
        
        Promotion promotion = promotionRepository.findById(promotionId)
            .orElseThrow(() -> new RuntimeException("Promotion not found with ID: " + promotionId));
        
        // Increment usage count
        promotion.setUsageCount(promotion.getUsageCount() + 1);
        promotionRepository.save(promotion);
        
        log.info("Promotion usage count incremented: promotionId={}, newCount={}", 
            promotionId, promotion.getUsageCount());
    }
}
