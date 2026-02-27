package com.example.project_backend04.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "promotions")
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String promotionId;

    @Column(nullable = false)
    private String title;

    @Column
    private String subtitle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private String image;

    @Column
    private String backgroundImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionType type;

    @Column(precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal minPurchaseAmount;

    @Column
    private LocalDate validFrom;

    @Column
    private LocalDate validUntil;

    @ElementCollection
    @CollectionTable(name = "promotion_features", joinColumns = @JoinColumn(name = "promotion_id"))
    @Column(name = "feature")
    private List<String> features;

    @Column
    private String ctaText;

    @Column
    private String ctaLink;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Boolean isFeatured = false;

    @Column
    private Integer usageLimit;

    @Column
    private Integer usageCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createDate;

    @Column
    private LocalDateTime updateDate;

    public enum PromotionType {
        PERCENTAGE_DISCOUNT,
        FIXED_AMOUNT_DISCOUNT,
        FREE_TRIAL,
        BONUS_DAYS,
        SPECIAL_OFFER
    }

    // Helper methods
    public boolean isValid() {
        LocalDate now = LocalDate.now();
        return isActive && 
               (validFrom == null || !now.isBefore(validFrom)) &&
               (validUntil == null || !now.isAfter(validUntil)) &&
               (usageLimit == null || usageCount < usageLimit);
    }

    public BigDecimal calculateDiscount(BigDecimal originalAmount) {
        if (!isValid()) return BigDecimal.ZERO;
        
        if (minPurchaseAmount != null && originalAmount.compareTo(minPurchaseAmount) < 0) {
            return BigDecimal.ZERO;
        }

        switch (type) {
            case PERCENTAGE_DISCOUNT:
                return originalAmount.multiply(discountPercentage).divide(BigDecimal.valueOf(100));
            case FIXED_AMOUNT_DISCOUNT:
                return discountAmount.min(originalAmount);
            default:
                return BigDecimal.ZERO;
        }
    }

    @PrePersist
    protected void onCreate() {
        this.createDate = LocalDateTime.now();
        this.updateDate = LocalDateTime.now();
        if (this.promotionId == null) {
            this.promotionId = "PROMO" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateDate = LocalDateTime.now();
    }
}