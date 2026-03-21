package com.example.project_backend04.dto.request.TrainerBooking;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreateServiceLinkedBookingRequest {
    
    @NotNull(message = "Service registration ID is required")
    private Long serviceRegistrationId;
    
    @NotNull(message = "Booking date is required")
    @Future(message = "Booking date must be in the future")
    private LocalDate bookingDate;
    
    @NotNull(message = "Start time is required")
    private LocalTime startTime;
    
    @NotNull(message = "End time is required")
    private LocalTime endTime;
    
    @Size(max = 500, message = "Session objective cannot exceed 500 characters")
    private String sessionObjective;
    
    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;
    
    private Integer sessionNumber;
    
    public boolean isValidTimeRange() {
        return startTime != null && endTime != null && startTime.isBefore(endTime);
    }
    
    public long getDurationMinutes() {
        if (!isValidTimeRange()) return 0;
        return java.time.Duration.between(startTime, endTime).toMinutes();
    }
    
    public boolean isValidDuration() {
        long duration = getDurationMinutes();
        return duration >= 30 && duration <= 480; // 30 minutes to 8 hours
    }
}