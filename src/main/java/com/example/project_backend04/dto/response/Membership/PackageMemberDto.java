package com.example.project_backend04.dto.response.Membership;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageMemberDto {
    private Long id;
    private MembershipUserDto user;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal paidAmount;
    private String status;
    private String orderId; // For invoice printing
}
