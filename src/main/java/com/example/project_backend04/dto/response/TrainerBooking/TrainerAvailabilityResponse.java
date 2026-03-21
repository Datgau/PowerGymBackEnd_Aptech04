package com.example.project_backend04.dto.response.TrainerBooking;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainerAvailabilityResponse {
    
    private Long trainerId;
    private String trainerName;
    private LocalDate date;
    private List<TimeSlot> bookedSlots;
    private List<TimeSlot> availableSlots;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSlot {
        private LocalTime startTime;
        private LocalTime endTime;
        private String bookingId; // Only for booked slots
        private String clientName; // Only for booked slots
    }
}