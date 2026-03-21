package com.example.project_backend04.dto.response.Trainer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerScheduleResponse {
    
    // Trainer Info
    private Long trainerId;
    private String trainerName;
    private String trainerEmail;
    private String trainerPhone;
    private String trainerAvatar;
    private boolean isActive;
    
    // Schedule Period
    private LocalDate fromDate;
    private LocalDate toDate;
    
    // Schedule Details
    private List<DailySchedule> dailySchedules;
    private int totalBookings;
    private int confirmedBookings;
    private int pendingBookings;
    private int completedBookings;
    
    // Availability Summary
    private List<String> availableDays; // ["MONDAY", "TUESDAY", ...]
    private LocalTime earliestStart;
    private LocalTime latestEnd;
    private double averageBookingsPerDay;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailySchedule {
        private LocalDate date;
        private String dayOfWeek;
        private List<BookingSlot> bookings;
        private List<AvailableSlot> availableSlots;
        private int totalBookings;
        private boolean hasConflicts;
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class BookingSlot {
            private Long bookingId;
            private LocalTime startTime;
            private LocalTime endTime;
            private String clientName;
            private String clientPhone;
            private String serviceName;
            private String status; // PENDING, CONFIRMED, COMPLETED, CANCELLED
            private String notes;
            private boolean isServiceLinked;
            private Long serviceRegistrationId;
        }
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class AvailableSlot {
            private LocalTime startTime;
            private LocalTime endTime;
            private int durationMinutes;
            private boolean isRecommended; // Based on trainer's typical schedule
        }
    }
    
    // Helper methods
    public double getBookingUtilizationRate() {
        if (totalBookings == 0) return 0.0;
        return (double) confirmedBookings / totalBookings * 100;
    }
    
    public boolean hasHighWorkload() {
        return averageBookingsPerDay > 6; // More than 6 bookings per day
    }
    
    public String getWorkloadStatus() {
        if (averageBookingsPerDay <= 2) return "LOW";
        else if (averageBookingsPerDay <= 5) return "MODERATE";
        else if (averageBookingsPerDay <= 8) return "HIGH";
        else return "OVERLOADED";
    }
}