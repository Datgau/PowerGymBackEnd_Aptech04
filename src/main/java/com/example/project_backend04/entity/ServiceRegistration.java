package com.example.project_backend04.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "service_registrations")
public class ServiceRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private GymService gymService;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationStatus status = RegistrationStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime registrationDate;

    @Column
    private LocalDateTime expirationDate;

    @Column
    private LocalDateTime cancelledDate;

    @Column
    private String cancellationReason;

    // NEW FIELDS for trainer integration
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id")
    private User trainer; // Selected trainer (nullable for backward compatibility)
    
    @Column
    private LocalDateTime trainerSelectedAt; // When trainer was selected
    
    @Column(columnDefinition = "TEXT")
    private String trainerSelectionNotes; // Notes about trainer selection
    
    // NEW RELATIONSHIP
    @OneToMany(mappedBy = "serviceRegistration", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TrainerBooking> trainerBookings = new ArrayList<>();

    public enum RegistrationStatus {
        ACTIVE,
        CANCELLED,
        COMPLETED,
        EXPIRED
    }

    @PrePersist
    protected void onCreate() {
        this.registrationDate = LocalDateTime.now();
        // Tự động tính expirationDate dựa trên service duration
        if (this.gymService != null && this.gymService.getDuration() != null) {
            this.expirationDate = this.registrationDate.plusDays(this.gymService.getDuration());
        }
    }
    
    // NEW HELPER METHODS
    public boolean isExpired() {
        return expirationDate != null && expirationDate.isBefore(LocalDateTime.now());
    }
    
    public RegistrationStatus getActualStatus() {
        if (isExpired() && status == RegistrationStatus.ACTIVE) {
            return RegistrationStatus.EXPIRED;
        }
        return status;
    }
    public boolean hasTrainer() {
        return trainer != null;
    }
    
    public boolean canBookTrainer() {
        return hasTrainer() && status == RegistrationStatus.ACTIVE;
    }
    
    public List<TrainerBooking> getActiveBookings() {
        if (trainerBookings == null) {
            return new ArrayList<>();
        }
        return trainerBookings.stream()
            .filter(booking -> booking.getStatus() == TrainerBooking.BookingStatus.CONFIRMED)
            .collect(Collectors.toList());
    }
    
    public List<TrainerBooking> getPendingBookings() {
        if (trainerBookings == null) {
            return new ArrayList<>();
        }
        return trainerBookings.stream()
            .filter(booking -> booking.getStatus() == TrainerBooking.BookingStatus.PENDING)
            .collect(Collectors.toList());
    }
    
    public int getTotalBookingsCount() {
        return trainerBookings != null ? trainerBookings.size() : 0;
    }
    
    public boolean hasAnyBookings() {
        return trainerBookings != null && !trainerBookings.isEmpty();
    }
}
