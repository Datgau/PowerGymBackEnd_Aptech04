package com.example.project_backend04.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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

    @Column(length = 255)
    private String password;

    @Column(unique = true, length = 255)
    private String email;

    @Column(length = 255)
    private String fullName;

    @Column(length = 50)
    private String phoneNumber;

    @Column(length = 50)
    private String dateOfBirth;

    @Column(length = 500)
    private String avatar;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 500)
    private String coverPhoto;
    
    @Column
    private Integer totalExperienceYears;
    
    @Column(length = 255)
    private String education;
    
    @Column(length = 255)
    private String emergencyContact;
    
    @Column(length = 50)
    private String emergencyPhone;

    @Column(precision = 15, scale = 2)
    private BigDecimal salaryBalance = BigDecimal.ZERO;

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

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<UserProvider> providers;


    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts;


    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Membership> memberships;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Booking> bookings;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CheckIn> checkIns;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceRegistration> serviceRegistrations;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrainerSpecialty> trainerSpecialties;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrainerDocument> trainerDocuments;

    @OneToMany(mappedBy = "trainer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrainerWorkingHours> workingHours;

    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImportReceipt> importReceipts;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductOrder> productOrders;

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
}
