package com.example.project_backend04.entity;

import com.example.project_backend04.enums.BookingStatus;
import com.example.project_backend04.enums.PaymentStatus;
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
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id", nullable = true)
    private User trainer;

    @Column(nullable = false)
    private LocalDate bookingDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String sessionType;

//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false, columnDefinition = "VARCHAR(20) CHECK (status IN ('PENDING','CONFIRMED','REJECTED','CANCELLED','COMPLETED','NO_SHOW','RESCHEDULED'))")
//    private BookingStatus status = BookingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime cancelledAt;

    @Column
    private String cancellationReason;


    @Column
    private LocalDateTime rejectedAt;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Column
    private String rejectionMediaUrl;


    @Column(nullable = false)
    @Builder.Default
    private Boolean isAssignedByAdmin = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_registration_id")
    private ServiceRegistration serviceRegistration;
    
    @Column(columnDefinition = "TEXT")
    private String sessionObjective;
    
    @Column
    private Integer sessionNumber;

    @Column(columnDefinition = "TEXT")
    private String trainerNotes;
    
    @Column(columnDefinition = "TEXT")
    private String clientFeedback;
    
    @Column
    private Integer rating;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_order_id")
    private PaymentOrder paymentOrder;

    public boolean canTrainerRespond() {
        return status == BookingStatus.PENDING;
    }

    public boolean canCancel() {
        return (status == BookingStatus.CONFIRMED || status == BookingStatus.PENDING) &&
               LocalDateTime.of(bookingDate, startTime)
               .isAfter(LocalDateTime.now().plusHours(2));
    }

    public boolean isUpcoming() {
        return (status == BookingStatus.CONFIRMED || status == BookingStatus.PENDING) &&
               LocalDateTime.of(bookingDate, startTime).isAfter(LocalDateTime.now());
    }

    public boolean isUnassigned() {
        return trainer == null;
    }

    public boolean isRejected() {
        return status == BookingStatus.REJECTED;
    }

    public boolean hasTimeConflict(LocalDate date, LocalTime start, LocalTime end) {
        if (!this.bookingDate.equals(date)) return false;
        if (this.status != BookingStatus.CONFIRMED && this.status != BookingStatus.PENDING) {
            return false;
        }
        return start.isBefore(this.endTime) && end.isAfter(this.startTime);
    }


    public boolean isLinkedToService() {
        return serviceRegistration != null;
    }

    public boolean canReschedule() {
        return (status == BookingStatus.CONFIRMED || status == BookingStatus.PENDING) &&
               LocalDateTime.of(bookingDate, startTime)
               .isAfter(LocalDateTime.now().plusHours(4));
    }

    public boolean requiresTrainerConfirmation() {
        return status == BookingStatus.PENDING && trainer != null;
    }

    public boolean isCompleted() {
        return status == BookingStatus.COMPLETED;
    }

    public boolean hasRating() {
        return rating != null && rating >= 1 && rating <= 5;
    }

    public String getServiceName() {
        if (isLinkedToService() && serviceRegistration.getGymService() != null) {
            return serviceRegistration.getGymService().getName();
        }
        return "Personal Training";
    }

    public String getPaymentStatus() {
        if (paymentOrder != null) {
            PaymentStatus status = paymentOrder.getStatus();
            if (status != null) {
                switch (status) {
                    case SUCCESS:
                        return "PAID";
                    case PENDING:
                        return "PENDING";
                    case FAILED:
                    case EXPIRED:
                        return "UNPAID";
                }
            }
        }
        
        // Fallback: Check payment through ServiceRegistration
        if (serviceRegistration != null && serviceRegistration.getGymService() != null) {
            // If service registration exists and is ACTIVE, assume service was paid
            if (serviceRegistration.getStatus() == com.example.project_backend04.enums.RegistrationStatus.ACTIVE) {
                return "PAID";
            }
        }
        
        return "UNPAID";
    }

    public Long getServiceRegistrationId() {
        return isLinkedToService() ? serviceRegistration.getId() : null;
    }

    public boolean wasAssignedByAdmin() {
        return Boolean.TRUE.equals(isAssignedByAdmin);
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.bookingId == null) {
            this.bookingId = "TB" + System.currentTimeMillis();
        }
        if (this.status == null) {
            this.status = BookingStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}