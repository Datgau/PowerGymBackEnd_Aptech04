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

    // Tìm document by ID và user (security check)
    @Query("SELECT td FROM TrainerDocument td WHERE td.id = :id AND td.user = :user AND td.isActive = true")
    Optional<TrainerDocument> findByIdAndUserAndIsActiveTrue(@Param("id") Long id, @Param("user") User user);
}