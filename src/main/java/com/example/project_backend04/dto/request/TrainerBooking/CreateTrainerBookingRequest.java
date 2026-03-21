package com.example.project_backend04.dto.request.TrainerBooking;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTrainerBookingRequest {
    
    @NotNull(message = "Trainer ID is required")
    private Long trainerId;
    
    @NotNull(message = "Booking date is required")
    private LocalDate bookingDate;
    
    @NotNull(message = "Start time is required")
    private LocalTime startTime;
    
    @NotNull(message = "End time is required")
    private LocalTime endTime;
    
    private String notes;
    
    private String sessionType;
}