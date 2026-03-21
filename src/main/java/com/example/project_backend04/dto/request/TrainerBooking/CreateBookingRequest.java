package com.example.project_backend04.dto.request.TrainerBooking;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBookingRequest {
    
    @NotNull(message = "Trainer ID is required")
    private Long trainerId;
    
    @NotNull(message = "Booking date is required")
    @Future(message = "Booking date must be in the future")
    private LocalDate bookingDate;
    
    @NotNull(message = "Start time is required")
    private LocalTime startTime;
    
    @NotNull(message = "End time is required")
    private LocalTime endTime;
    
    private String notes;
    private String sessionType;
    private Long excludeBookingId; // For rescheduling validation
    
    public boolean isValidTimeRange() {
        return startTime != null && endTime != null && startTime.isBefore(endTime);
    }
    
    public long getDurationMinutes() {
        if (!isValidTimeRange()) return 0;
        return java.time.Duration.between(startTime, endTime).toMinutes();
    }
    
    public boolean isMinimumDuration() {
        return getDurationMinutes() >= 30; // Minimum 30 minutes
    }
    
    public boolean isMaximumDuration() {
        return getDurationMinutes() <= 480; // Maximum 8 hours
    }
}