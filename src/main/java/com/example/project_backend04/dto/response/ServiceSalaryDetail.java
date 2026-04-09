package com.example.project_backend04.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceSalaryDetail {
    private Long serviceId;
    private String serviceName;
    private Integer studentCount;
    private BigDecimal servicePrice;
    private BigDecimal trainerPercentage;
    private BigDecimal salaryAmount;
}
