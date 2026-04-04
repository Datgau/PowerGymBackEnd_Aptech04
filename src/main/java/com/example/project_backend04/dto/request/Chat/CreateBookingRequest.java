package com.example.project_backend04.dto.request.Chat;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateBookingRequest(
        Long trainerId,
        LocalDate bookingDate,
        LocalTime startTime,
        LocalTime endTime,
        String notes,
        String sessionType
) {

    public boolean isValidTimeRange() {
        return startTime != null && endTime != null && startTime.isBefore(endTime);
    }


    public boolean isMinimumDuration() {
        if (startTime == null || endTime == null) return false;
        return Duration.between(startTime, endTime).toMinutes() >= 30;
    }

    public boolean isMaximumDuration() {
        if (startTime == null || endTime == null) return false;
        return Duration.between(startTime, endTime).toHours() <= 8;
    }
}
