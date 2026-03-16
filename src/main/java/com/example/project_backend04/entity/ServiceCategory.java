package com.example.project_backend04.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "service_categories")
public class ServiceCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name; // PERSONAL_TRAINER, BOXING, YOGA, etc.

    @Column(nullable = false, length = 200)
    private String displayName; // "Personal Trainer", "Boxing", "Yoga", etc.

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private String icon; // Icon class or URL

    @Column
    private String color; // Hex color code for UI

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Integer sortOrder = 0; // For ordering in UI

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GymService> gymServices;

    @OneToMany(mappedBy = "specialty", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TrainerSpecialty> trainerSpecialties;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public String getNameUpperCase() {
        return this.name != null ? this.name.toUpperCase() : null;
    }

    public boolean isPersonalTrainer() {
        return "PERSONAL_TRAINER".equals(this.name);
    }

    public boolean isBoxing() {
        return "BOXING".equals(this.name);
    }

    public boolean isYoga() {
        return "YOGA".equals(this.name);
    }
}