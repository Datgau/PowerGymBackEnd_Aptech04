package com.example.project_backend04.repository;

import com.example.project_backend04.entity.PromotionUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, Long> {
    
    @Query("SELECT COUNT(pu) FROM PromotionUsage pu WHERE pu.promotionId = :promotionId AND pu.userId = :userId")
    long countByPromotionIdAndUserId(@Param("promotionId") Long promotionId, @Param("userId") Long userId);
    
    @Query("SELECT pu FROM PromotionUsage pu WHERE pu.userId = :userId ORDER BY pu.usedAt DESC")
    List<PromotionUsage> findByUserIdOrderByUsedAtDesc(@Param("userId") Long userId);
    
    @Query("SELECT pu FROM PromotionUsage pu WHERE pu.promotionId = :promotionId ORDER BY pu.usedAt DESC")
    List<PromotionUsage> findByPromotionIdOrderByUsedAtDesc(@Param("promotionId") Long promotionId);
    
    @Query("SELECT COUNT(pu) FROM PromotionUsage pu WHERE pu.promotionId = :promotionId")
    long countByPromotionId(@Param("promotionId") Long promotionId);
}
