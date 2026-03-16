package com.example.project_backend04.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "trainer_specialties", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "specialty_id"}))
public class TrainerSpecialty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialty_id", nullable = false)
    private ServiceCategory specialty;

    @Column(columnDefinition = "TEXT")
    private String description; // Mô tả về chuyên môn của trainer trong bộ môn này

    @Column
    private Integer experienceYears; // Số năm kinh nghiệm trong bộ môn này

    @Column(columnDefinition = "TEXT")
    private String certifications; // Các chứng chỉ liên quan đến bộ môn này

    @Column
    private String level; // Trình độ: BEGINNER, INTERMEDIATE, ADVANCED, EXPERT

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}