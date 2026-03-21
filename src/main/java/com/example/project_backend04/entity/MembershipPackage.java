package com.example.project_backend04.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "membership_packages")
public class MembershipPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String packageId; // sẽ được tự động generate

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(nullable = false)
    private Integer duration;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(precision = 10, scale = 2)
    private BigDecimal originalPrice;

    @Column
    private Integer discount;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "package_features", joinColumns = @JoinColumn(name = "package_id"))
    @Column(name = "feature")
    private List<String> features;

    @Column(nullable = false)
    private Boolean isPopular = false;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column
    private String color;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createDate;

    @Column
    private LocalDateTime updateDate;

    @OneToMany(mappedBy = "membershipPackage", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Membership> memberships;

    @PrePersist
    protected void onCreate() {
        this.createDate = LocalDateTime.now();
        this.updateDate = LocalDateTime.now();
        
        // Tự động generate packageId nếu chưa có
        if (this.packageId == null || this.packageId.trim().isEmpty()) {
            this.packageId = generateUniquePackageId();
        }
    }
    
    private String generateUniquePackageId() {
        if (this.name == null || this.name.trim().isEmpty()) {
            return "PKG_" + System.currentTimeMillis() % 10000;
        }
        
        String baseName = this.name.toUpperCase()
            .replaceAll("[^A-Z0-9\\s]", "") // Loại bỏ ký tự đặc biệt
            .replaceAll("\\s+", "_") // Thay khoảng trắng bằng underscore
            .trim();
            
        if (baseName.length() > 15) {
            baseName = baseName.substring(0, 15);
        }
        
        if (baseName.isEmpty()) {
            baseName = "PKG";
        }
        
        return baseName + "_" + (System.currentTimeMillis() % 10000);
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateDate = LocalDateTime.now();
    }
}