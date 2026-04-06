package com.example.project_backend04.repository;

import com.example.project_backend04.entity.Promotion;
import com.example.project_backend04.enums.PromotionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    @Query("SELECT p FROM Promotion p WHERE p.code = :code")
    Optional<Promotion> findByCode(@Param("code") String code);
    
    @Query("SELECT p FROM Promotion p WHERE p.isActive = true " +
           "AND (p.validFrom IS NULL OR p.validFrom <= :now) " +
           "AND (p.validUntil IS NULL OR p.validUntil >= :now)")
    List<Promotion> findActivePromotions(@Param("now") LocalDateTime now);
    
    @Query("SELECT p FROM Promotion p WHERE p.isActive = true " +
           "AND (p.validFrom IS NULL OR p.validFrom <= :now) " +
           "AND (p.validUntil IS NULL OR p.validUntil >= :now) " +
           "AND (p.usageLimit IS NULL OR p.usageCount < p.usageLimit) " +
           "AND p.type = :type")
    List<Promotion> findActivePromotionsByType(
        @Param("now") LocalDateTime now,
        @Param("type") PromotionType type
    );
    
    @Query("SELECT p FROM Promotion p WHERE p.isFeatured = true AND p.isActive = true ORDER BY p.createdAt DESC")
    List<Promotion> findFeaturedPromotions();
}
