package com.example.project_backend04.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainerDashboardResponse {
    private Long trainerId;
    private String trainerName;
    private String trainerEmail;
    private String trainerAvatar;
    
    // Statistics
    private Integer totalClients;
    private Integer pendingApprovals;
    private Integer confirmedBookings;
    private Integer completedBookings;
    
    // Salary information (excluding rejected and cancelled)
    private BigDecimal totalSalary;
    private BigDecimal currentBalance;
    private List<ServiceSalaryDetail> serviceBreakdown;
    
    private LocalDateTime calculatedAt;
}
