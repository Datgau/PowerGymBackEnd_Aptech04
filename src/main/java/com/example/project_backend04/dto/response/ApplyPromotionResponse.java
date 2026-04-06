package com.example.project_backend04.dto.response;

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
public class ApplyPromotionResponse {
    private Boolean success;
    private String message;
    private Long promotionId;
    private String promotionCode;
    private String promotionName;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    
    // Debug information (only populated on error)
    private DebugInfo debugInfo;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DebugInfo {
        private Boolean isActive;
        private LocalDateTime validFrom;
        private LocalDateTime validUntil;
        private LocalDateTime currentTime;
        private Integer usageCount;
        private Integer usageLimit;
        private BigDecimal minPurchaseAmount;
        private BigDecimal orderAmount;
        private String failureReason;
    }
}
