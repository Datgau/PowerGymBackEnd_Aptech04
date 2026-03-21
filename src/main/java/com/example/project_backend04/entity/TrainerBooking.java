package com.example.project_backend04.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "trainer_bookings")
public class TrainerBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String bookingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Client who books

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id", nullable = false)
    private User trainer; // Trainer (User with TRAINER role)

    @Column(nullable = false)
    private LocalDate bookingDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String sessionType; // Personal Training, Group Session, etc.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.CONFIRMED;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime cancelledAt;

    @Column
    private String cancellationReason;

    // NEW FIELDS for service integration
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_registration_id")
    private ServiceRegistration serviceRegistration; // Link to service registration
    
    @Column(columnDefinition = "TEXT")
    private String sessionObjective; // Objective for this session
    
    @Column
    private Integer sessionNumber; // Session number in the service program
    
    @Column(columnDefinition = "TEXT")
    private String trainerNotes; // Trainer's notes about the session
    
    @Column(columnDefinition = "TEXT")
    private String clientFeedback; // Client feedback after session
    
    @Column
    private Integer rating; // Session rating (1-5)

    public enum BookingStatus {
        PENDING,     // Waiting for trainer confirmation
        CONFIRMED,   // Confirmed by trainer
        CANCELLED,   // Cancelled by either party
        COMPLETED,   // Session completed
        NO_SHOW,     // Client didn't show up
        RESCHEDULED  // Rescheduled to another time
    }

    // Helper methods
    public boolean canCancel() {
        return (status == BookingStatus.CONFIRMED || status == BookingStatus.PENDING) && 
               LocalDateTime.of(bookingDate, startTime)
               .isAfter(LocalDateTime.now().plusHours(2)); // Can cancel 2 hours before session
    }

    public boolean isUpcoming() {
        return (status == BookingStatus.CONFIRMED || status == BookingStatus.PENDING) && 
               LocalDateTime.of(bookingDate, startTime).isAfter(LocalDateTime.now());
    }

    public boolean hasTimeConflict(LocalDate date, LocalTime start, LocalTime end) {
        if (!this.bookingDate.equals(date) || this.status != BookingStatus.CONFIRMED) {
            return false;
        }
        // Check if time ranges overlap: (start1 < end2) AND (end1 > start2)
        return start.isBefore(this.endTime) && end.isAfter(this.startTime);
    }
    
    // NEW HELPER METHODS for service integration
    public boolean isLinkedToService() {
        return serviceRegistration != null;
    }
    
    public boolean canReschedule() {
        return (status == BookingStatus.CONFIRMED || status == BookingStatus.PENDING) && 
               LocalDateTime.of(bookingDate, startTime)
               .isAfter(LocalDateTime.now().plusHours(4));
    }
    
    public boolean requiresTrainerConfirmation() {
        return status == BookingStatus.PENDING;
    }
    
    public boolean isCompleted() {
        return status == BookingStatus.COMPLETED;
    }
    
    public boolean hasRating() {
        return rating != null && rating >= 1 && rating <= 5;
    }
    
    public String getServiceName() {
        return isLinkedToService() ? serviceRegistration.getGymService().getName() : "Personal Training";
    }
    
    public Long getServiceRegistrationId() {
        return isLinkedToService() ? serviceRegistration.getId() : null;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.bookingId == null) {
            this.bookingId = "TB" + System.currentTimeMillis();
        }
        // Set default status to PENDING for new bookings
        if (this.status == null) {
            this.status = BookingStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}