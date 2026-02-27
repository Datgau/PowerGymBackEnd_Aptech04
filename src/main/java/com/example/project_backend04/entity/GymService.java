package com.example.project_backend04.entity;

import com.example.project_backend04.enums.ServiceCategory;
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

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
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

    @Column(nullable = false, updatable = false)
    private LocalDateTime createDate;

    @Column
    private LocalDateTime updateDate;

    @OneToMany(mappedBy = "gymService", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClassSchedule> schedules;



    @PrePersist
    protected void onCreate() {
        this.createDate = LocalDateTime.now();
        this.updateDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateDate = LocalDateTime.now();
    }
}