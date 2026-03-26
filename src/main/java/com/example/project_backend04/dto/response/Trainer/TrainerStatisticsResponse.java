package com.example.project_backend04.dto.response.Trainer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerStatisticsResponse {
    
    // Trainer Info
    private Long trainerId;
    private String trainerName;
    private String trainerEmail;
    private boolean isActive;
    
    // Period
    private LocalDate fromDate;
    private LocalDate toDate;
    private int totalDays;
    
    // Booking Statistics
    private int totalBookings;
    private int confirmedBookings;
    private int completedBookings;
    private int cancelledBookings;
    private int pendingBookings;
    private int rejectedBookings;
    
    // Performance Metrics
    private double completionRate; // completed / confirmed * 100
    private double confirmationRate; // confirmed / total * 100
    private double averageRating;
    private int totalRatings;
    private double averageBookingsPerDay;
    
    // Revenue & Service Statistics
    private double totalRevenue;
    private double averageRevenuePerBooking;
    private Map<String, Integer> serviceBreakdown;
    private Map<String, Double> serviceRevenue;
    // Time Analysis
    private Map<String, Integer> bookingsByDayOfWeek;
    private Map<Integer, Integer> bookingsByHour;
    private int totalWorkingHours;
    private double averageSessionDuration; // in minutes
    
    // Client Statistics
    private int uniqueClients;
    private int returningClients;
    private double clientRetentionRate;
    private List<TopClient> topClients;
    
    // Monthly Trends (if period > 1 month)
    private List<MonthlyData> monthlyTrends;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopClient {
        private String clientName;
        private String clientEmail;
        private int totalBookings;
        private double totalSpent;
        private LocalDate lastBooking;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyData {
        private int year;
        private int month;
        private String monthName;
        private int totalBookings;
        private int completedBookings;
        private double revenue;
        private double averageRating;
    }
    
    // Helper methods
    public String getPerformanceGrade() {
        double score = (completionRate * 0.4) + (confirmationRate * 0.3) + (averageRating * 20 * 0.3);
        if (score >= 90) return "EXCELLENT";
        else if (score >= 80) return "GOOD";
        else if (score >= 70) return "AVERAGE";
        else if (score >= 60) return "BELOW_AVERAGE";
        else return "POOR";
    }
    
    public boolean isHighPerformer() {
        return completionRate >= 90 && confirmationRate >= 85 && averageRating >= 4.0;
    }
    
    public String getWorkloadLevel() {
        if (averageBookingsPerDay <= 2) return "LOW";
        else if (averageBookingsPerDay <= 5) return "MODERATE";
        else if (averageBookingsPerDay <= 8) return "HIGH";
        else return "OVERLOADED";
    }
    
    public double getUtilizationRate() {
        // Assuming 8 hours working day, 60 minutes average session
        double maxPossibleBookings = totalDays * 8;
        return totalBookings / maxPossibleBookings * 100;
    }
}