package com.example.project_backend04.dto.response.TrainerBooking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlot {
    
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean isAvailable;
    private String description;
    
    public Duration getDuration() {
        return Duration.between(startTime, endTime);
    }
    
    public long getDurationMinutes() {
        return getDuration().toMinutes();
    }
    
    public boolean isValidSlot() {
        return startTime != null && endTime != null && startTime.isBefore(endTime);
    }
    
    public boolean overlaps(TimeSlot other) {
        if (other == null || !this.date.equals(other.date)) {
            return false;
        }
        return this.startTime.isBefore(other.endTime) && this.endTime.isAfter(other.startTime);
    }
    
    public static TimeSlot of(LocalDate date, LocalTime startTime, LocalTime endTime) {
        return TimeSlot.builder()
            .date(date)
            .startTime(startTime)
            .endTime(endTime)
            .isAvailable(true)
            .build();
    }
    
    public static TimeSlot unavailable(LocalDate date, LocalTime startTime, LocalTime endTime, String reason) {
        return TimeSlot.builder()
            .date(date)
            .startTime(startTime)
            .endTime(endTime)
            .isAvailable(false)
            .description(reason)
            .build();
    }
}