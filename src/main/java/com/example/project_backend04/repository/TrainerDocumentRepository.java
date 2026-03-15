package com.example.project_backend04.repository;

import com.example.project_backend04.entity.TrainerDocument;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrainerDocumentRepository extends JpaRepository<TrainerDocument, Long> {

    // Tìm tất cả documents của trainer
    @Query("SELECT td FROM TrainerDocument td WHERE td.user = :user AND td.isActive = true ORDER BY td.createdAt DESC")
    List<TrainerDocument> findByUserAndIsActiveTrueOrderByCreatedAtDesc(@Param("user") User user);

    // Tìm document theo type của trainer
    @Query("SELECT td FROM TrainerDocument td WHERE td.user = :user AND td.documentType = :documentType AND td.isActive = true")
    List<TrainerDocument> findByUserAndDocumentTypeAndIsActiveTrue(@Param("user") User user, @Param("documentType") DocumentType documentType);

    // Tìm documents chưa được verify
    @Query("SELECT td FROM TrainerDocument td WHERE td.isVerified = false AND td.isActive = true ORDER BY td.createdAt ASC")
    List<TrainerDocument> findUnverifiedDocuments();

    // Tìm documents sắp hết hạn (trong 30 ngày)
    @Query("SELECT td FROM TrainerDocument td WHERE td.expiryDate IS NOT NULL AND td.expiryDate BETWEEN :now AND :thirtyDaysLater AND td.isActive = true")
    List<TrainerDocument> findDocumentsExpiringWithin30Days(@Param("now") LocalDateTime now, @Param("thirtyDaysLater") LocalDateTime thirtyDaysLater);

    // Tìm documents đã hết hạn
    @Query("SELECT td FROM TrainerDocument td WHERE td.expiryDate IS NOT NULL AND td.expiryDate < :now AND td.isActive = true")
    List<TrainerDocument> findExpiredDocuments(@Param("now") LocalDateTime now);

    // Đếm documents của trainer theo type
    long countByUserAndDocumentTypeAndIsActiveTrue(User user, DocumentType documentType);

    // Kiểm tra trainer có document type cụ thể chưa
    boolean existsByUserAndDocumentTypeAndIsActiveTrue(User user, DocumentType documentType);

    // Tìm document by ID và user (security check)
    @Query("SELECT td FROM TrainerDocument td WHERE td.id = :id AND td.user = :user AND td.isActive = true")
    Optional<TrainerDocument> findByIdAndUserAndIsActiveTrue(@Param("id") Long id, @Param("user") User user);
}