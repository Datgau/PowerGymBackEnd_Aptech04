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
public class TrainerSalaryResponse {
    private Long trainerId;
    private String trainerName;
    private BigDecimal totalSalary;
    private List<ServiceSalaryDetail> serviceBreakdown;
    private LocalDateTime calculatedAt;
}
