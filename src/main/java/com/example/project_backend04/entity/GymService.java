package com.example.project_backend04.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "gym_services")
public class GymService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ServiceCategory category;
    @OneToMany(
            mappedBy = "gymService",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<GymServiceImage> images = new ArrayList<>();


    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column
    private Integer duration;

    @Column
    private Integer maxParticipants;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(precision = 3, scale = 2, nullable = false)
    private BigDecimal trainerPercentage = new BigDecimal("0.3");

    @Column(nullable = false, updatable = false)
    private LocalDateTime createDate;

    @Column
    private LocalDateTime updateDate;

    @OneToMany(mappedBy = "gymService", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClassSchedule> schedules;

    @OneToMany(mappedBy = "gymService", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceRegistration> registrations;



    @PrePersist
    protected void onCreate() {
        this.createDate = LocalDateTime.now();
        this.updateDate = LocalDateTime.now();
        validateTrainerPercentage();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateDate = LocalDateTime.now();
        validateTrainerPercentage();
    }

    protected void validateTrainerPercentage() {
        if (trainerPercentage != null) {
            if (trainerPercentage.compareTo(BigDecimal.ZERO) < 0 || 
                trainerPercentage.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException(
                    "Trainer percentage must be between 0.0 and 1.0"
                );
            }
        }
    }
}