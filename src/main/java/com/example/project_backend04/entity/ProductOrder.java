package com.example.project_backend04.entity;

import com.example.project_backend04.enums.DeliveryStatus;
import com.example.project_backend04.enums.SaleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(nullable = false, length = 255)
    private String customerName;
    
    @Column(nullable = false, length = 50)
    private String customerPhone;
    
    @Column(length = 500)
    private String customerAddress;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SaleType saleType;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus deliveryStatus;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Column(length = 255)
    private String paymentId;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "productOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductOrderItem> items = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        // Set initial delivery status based on sale type
        if (saleType == SaleType.COUNTER) {
            this.deliveryStatus = DeliveryStatus.DELIVERED;
        } else {
            this.deliveryStatus = DeliveryStatus.PENDING;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public void calculateTotalAmount() {
        this.totalAmount = items.stream()
            .map(ProductOrderItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public boolean canUpdateDeliveryStatus() {
        return saleType == SaleType.ONLINE;
    }
}
