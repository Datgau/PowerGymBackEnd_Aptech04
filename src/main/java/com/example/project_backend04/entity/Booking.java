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
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String bookingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private ClassSchedule classSchedule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.CONFIRMED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime bookingDate;

    @Column
    private LocalDateTime cancelledDate;

    @Column
    private String cancellationReason;

    @Column
    private LocalDateTime attendedDate;

    public enum BookingStatus {
        CONFIRMED,
        CANCELLED,
        ATTENDED,
        NO_SHOW
    }

    // Helper methods
    public boolean canCancel() {
        return status == BookingStatus.CONFIRMED && 
               LocalDateTime.of(classSchedule.getDate(), classSchedule.getStartTime())
               .isAfter(LocalDateTime.now().plusHours(2)); // Can cancel 2 hours before class
    }

    @PrePersist
    protected void onCreate() {
        this.bookingDate = LocalDateTime.now();
        if (this.bookingId == null) {
            this.bookingId = "BK" + System.currentTimeMillis();
        }
    }
}