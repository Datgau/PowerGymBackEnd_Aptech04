package com.example.project_backend04.repository;

import com.example.project_backend04.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    
    @Query("SELECT p FROM Promotion p WHERE p.isActive = true AND (p.validFrom IS NULL OR p.validFrom <= :currentDate) AND (p.validUntil IS NULL OR p.validUntil >= :currentDate) AND (p.usageLimit IS NULL OR p.usageCount < p.usageLimit) ORDER BY p.createDate DESC")
    List<Promotion> findActivePromotions(@Param("currentDate") LocalDate currentDate);
    
    @Query("SELECT p FROM Promotion p WHERE p.isFeatured = true AND p.isActive = true AND (p.validFrom IS NULL OR p.validFrom <= :currentDate) AND (p.validUntil IS NULL OR p.validUntil >= :currentDate) ORDER BY p.createDate DESC")
    List<Promotion> findFeaturedPromotions(@Param("currentDate") LocalDate currentDate);
    
    List<Promotion> findByTypeAndIsActiveTrue(Promotion.PromotionType type);
    
    Optional<Promotion> findByPromotionIdAndIsActiveTrue(String promotionId);
    
    @Query("SELECT p FROM Promotion p WHERE p.validUntil = :date AND p.isActive = true")
    List<Promotion> findExpiringPromotions(@Param("date") LocalDate date);
    
    @Query("SELECT p FROM Promotion p WHERE p.validUntil < :date AND p.isActive = true")
    List<Promotion> findExpiredPromotions(@Param("date") LocalDate date);
    
    @Modifying
    @Query("UPDATE Promotion p SET p.usageCount = p.usageCount + 1 WHERE p.id = :id")
    void incrementUsageCount(@Param("id") Long id);
    
    @Query("SELECT p FROM Promotion p WHERE p.isActive = true ORDER BY p.createDate DESC")
    List<Promotion> findAllActiveOrderByCreateDate();
}