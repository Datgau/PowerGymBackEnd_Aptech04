package com.example.project_backend04.entity;

import com.example.project_backend04.enums.PromotionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
@Entity
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private String title;

    private String subtitle;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String image;

    private String backgroundImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PromotionType type;

    // % giảm
    @Column(precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    // giảm cố định
    @Column(precision = 10, scale = 2)
    private BigDecimal discountAmount;

    // giảm tối đa (áp dụng cho %)
    @Column(precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    // giá tối thiểu để apply
    @Column(precision = 10, scale = 2)
    private BigDecimal minPurchaseAmount;

    // thời gian hiệu lực
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;

    // UI display
    @ElementCollection
    @CollectionTable(name = "promotion_features", joinColumns = @JoinColumn(name = "promotion_id"))
    @Column(name = "feature")
    private List<String> features;

    private String ctaText;
    private String ctaLink;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean isFeatured = false;

    private Integer usageLimit;

    @Column(nullable = false)
    @Builder.Default
    private Integer usageCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // ===== VALIDATION =====
    public boolean isValid(BigDecimal orderAmount) {
        LocalDateTime now = LocalDateTime.now();

        return Boolean.TRUE.equals(isActive)
                && (validFrom == null || !now.isBefore(validFrom))
                && (validUntil == null || !now.isAfter(validUntil))
                && (usageLimit == null || usageCount < usageLimit)
                && (minPurchaseAmount == null || orderAmount.compareTo(minPurchaseAmount) >= 0);
    }

    // ===== CALCULATE DISCOUNT =====
    public BigDecimal calculateDiscount(BigDecimal originalAmount) {
        if (originalAmount == null || !isValid(originalAmount)) {
            return BigDecimal.ZERO;
        }

        switch (type) {
            case PERCENTAGE_DISCOUNT:
                if (discountPercentage == null) return BigDecimal.ZERO;

                BigDecimal discount = originalAmount
                        .multiply(discountPercentage)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                if (maxDiscountAmount != null) {
                    discount = discount.min(maxDiscountAmount);
                }

                return discount;

            case FIXED_AMOUNT_DISCOUNT:
                if (discountAmount == null) return BigDecimal.ZERO;
                return discountAmount.min(originalAmount);

            default:
                return BigDecimal.ZERO;
        }
    }

    // ===== APPLY DISCOUNT =====
    public BigDecimal applyDiscount(BigDecimal originalAmount) {
        BigDecimal discount = calculateDiscount(originalAmount);
        return originalAmount.subtract(discount).max(BigDecimal.ZERO);
    }

    // ===== LIFECYCLE =====
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.code == null) {
            this.code = "PROMO" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}