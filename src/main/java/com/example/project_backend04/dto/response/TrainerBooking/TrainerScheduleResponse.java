package com.example.project_backend04.dto.response.TrainerBooking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerScheduleResponse {
    
    private Long trainerId;
    private String trainerName;
    private LocalDate fromDate;
    private LocalDate toDate;
    private List<TrainerBookingResponse> bookings;
    private int totalBookings;
    private int confirmedBookings;
    private int pendingBookings;
    private List<TimeSlot> availableSlots;
    
    public Map<LocalDate, List<TrainerBookingResponse>> getBookingsByDate() {
        if (bookings == null) {
            return Map.of();
        }
        
        return bookings.stream()
            .collect(Collectors.groupingBy(TrainerBookingResponse::getBookingDate));
    }
    
    public List<TrainerBookingResponse> getBookingsForDate(LocalDate date) {
        return bookings != null ? 
            bookings.stream()
                .filter(booking -> booking.getBookingDate().equals(date))
                .collect(Collectors.toList()) : 
            List.of();
    }
    
    public boolean hasBookingsOnDate(LocalDate date) {
        return !getBookingsForDate(date).isEmpty();
    }
    
    public int getBookingCountForDate(LocalDate date) {
        return getBookingsForDate(date).size();
    }
    
    public boolean isAvailableOnDate(LocalDate date) {
        // This would check against working hours and existing bookings
        // For now, just check if there are any available slots
        return availableSlots != null && 
               availableSlots.stream().anyMatch(slot -> slot.getDate().equals(date));
    }
    
    public List<LocalDate> getBusyDates() {
        if (bookings == null) {
            return List.of();
        }
        
        return bookings.stream()
            .map(TrainerBookingResponse::getBookingDate)
            .distinct()
            .collect(Collectors.toList());
    }
    
    public double getScheduleUtilization() {
        if (totalBookings == 0) return 0.0;
        
        // Calculate based on available time slots vs booked slots
        // This is a simplified calculation
        long totalDays = fromDate.until(toDate).getDays() + 1;
        double maxPossibleBookings = totalDays * 8; // Assuming 8 possible slots per day
        
        return Math.min(100.0, (totalBookings / maxPossibleBookings) * 100.0);
    }
}