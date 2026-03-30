package com.example.project_backend04.dto.response.TrainerBooking;

import com.example.project_backend04.dto.response.User.UserResponse;
import com.example.project_backend04.entity.TrainerBooking;
import com.example.project_backend04.enums.BookingStatus;
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
public class TrainerBookingResponse {
    
    private Long id;
    private String bookingId;
    private UserResponse user;
    private UserResponse trainer;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String notes;
    private String sessionType;
    private BookingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;

    // Rejection fields (trainer từ chối)
    private LocalDateTime rejectedAt;
    private String rejectionReason;
    private String rejectionMediaUrl;

    // Admin assignment
    private Boolean isAssignedByAdmin;

    // Service integration fields
    private Long serviceRegistrationId;
    private String serviceName;
    private String sessionObjective;
    private Integer sessionNumber;
    private String trainerNotes;
    private String clientFeedback;
    private Integer rating;
    
    // Payment status
    private String paymentStatus;
    
    public boolean isUpcoming() {
        return (status == BookingStatus.CONFIRMED ||
                status == BookingStatus.PENDING) &&
               LocalDateTime.of(bookingDate, startTime).isAfter(LocalDateTime.now());
    }
    
    public boolean canCancel() {
        return (status == BookingStatus.CONFIRMED ||
                status == BookingStatus.PENDING) &&
               LocalDateTime.of(bookingDate, startTime)
               .isAfter(LocalDateTime.now().plusHours(2));
    }
    
    public boolean canReschedule() {
        return (status == BookingStatus.CONFIRMED ||
                status == BookingStatus.PENDING) &&
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
    
    public boolean isLinkedToService() {
        return serviceRegistrationId != null;
    }
    
    public long getDurationMinutes() {
        if (startTime == null || endTime == null) return 0;
        return java.time.Duration.between(startTime, endTime).toMinutes();
    }
    
    public String getFormattedDuration() {
        long minutes = getDurationMinutes();
        if (minutes < 60) {
            return minutes + " minutes";
        }
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        if (remainingMinutes == 0) {
            return hours + " hour" + (hours > 1 ? "s" : "");
        }
        return hours + "h " + remainingMinutes + "m";
    }
    


    public boolean isRejected() {
        return status == BookingStatus.REJECTED;
    }

    public boolean isUnassigned() {
        return trainer == null;
    }
}