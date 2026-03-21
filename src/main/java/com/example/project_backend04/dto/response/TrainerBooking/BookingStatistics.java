package com.example.project_backend04.dto.response.TrainerBooking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingStatistics {
    
    private Long trainerId;
    private LocalDate fromDate;
    private LocalDate toDate;
    
    // Booking counts
    private Long totalBookings;
    private Long completedBookings;
    private Long cancelledBookings;
    private Long noShowBookings;
    private Long pendingBookings;
    
    // Ratings
    private Double averageRating;
    private Long totalRatings;
    private Map<Integer, Long> ratingDistribution; // rating -> count
    
    // Time statistics
    private Long totalHoursTrained;
    private Double averageSessionDuration;
    
    // Monthly breakdown
    private Map<String, Long> monthlyBookings; // "YYYY-MM" -> count
    
    // Performance metrics
    private Double completionRate; // completed / (completed + cancelled + no-show)
    private Double noShowRate; // no-show / total
    private Double cancellationRate; // cancelled / total
    
    // Revenue (if applicable)
    private Double totalRevenue;
    private Double averageRevenuePerSession;
    
    public double getCompletionRatePercentage() {
        return completionRate != null ? completionRate * 100 : 0.0;
    }
    
    public double getNoShowRatePercentage() {
        return noShowRate != null ? noShowRate * 100 : 0.0;
    }
    
    public double getCancellationRatePercentage() {
        return cancellationRate != null ? cancellationRate * 100 : 0.0;
    }
    
    public boolean hasRatings() {
        return totalRatings != null && totalRatings > 0;
    }
    
    public String getFormattedAverageRating() {
        if (hasRatings()) {
            return String.format("%.1f", averageRating);
        }
        return "No ratings";
    }
    
    public String getPerformanceLevel() {
        if (completionRate == null) return "Unknown";
        
        if (completionRate >= 0.95) return "Excellent";
        if (completionRate >= 0.85) return "Very Good";
        if (completionRate >= 0.75) return "Good";
        if (completionRate >= 0.65) return "Fair";
        return "Needs Improvement";
    }
}