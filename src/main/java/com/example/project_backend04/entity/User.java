package com.example.project_backend04.entity;

import com.example.project_backend04.enums.ServiceCategory;
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
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String password;

    @Column(unique = true)
    private String email;

    @Column
    private String fullName;

    @Column
    private String phoneNumber;

    @Column
    private String avatar;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column
    private String coverPhoto;

    @Column(length = 512)
    private String refreshToken;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createDate;

    @Column
    private LocalDateTime refreshTokenExpiryTime;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    // Một user có thể liên kết nhiều providers (Facebook, Google,…)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserProvider> providers;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts;


    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications;

    // PowerGym specific relationships
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Membership> memberships;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Booking> bookings;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CheckIn> checkIns;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceRegistration> serviceRegistrations;

    // Trainer specialties (only for users with TRAINER role)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrainerSpecialty> trainerSpecialties;

    // Trainer documents (only for users with TRAINER role)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrainerDocument> trainerDocuments;

    @PrePersist
    protected void onCreate() {
        this.createDate = LocalDateTime.now();
    }

    // Helper methods
    public boolean isTrainer() {
        return this.role != null && "TRAINER".equals(this.role.getName());
    }

    public boolean isAdmin() {
        return this.role != null && "ADMIN".equals(this.role.getName());
    }

    public boolean isUser() {
        return this.role != null && "USER".equals(this.role.getName());
    }

    public boolean hasTrainerSpecialties() {
        return this.trainerSpecialties != null && !this.trainerSpecialties.isEmpty();
    }

    public boolean canTeachCategory(ServiceCategory category) {
        if (!isTrainer()) return false;
        return this.trainerSpecialties != null && this.trainerSpecialties.stream()
                .anyMatch(specialty -> specialty.getSpecialty() == category && specialty.getIsActive());
    }
}
