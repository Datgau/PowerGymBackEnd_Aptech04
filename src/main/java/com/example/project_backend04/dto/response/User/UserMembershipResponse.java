package com.example.project_backend04.dto.response.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMembershipResponse {
    private Long id;
    private MembershipPackageInfo membershipPackage;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
    private BigDecimal paidAmount;
    private String status;
    private String paymentMethod;
    private String registrationDate;
    private String orderId;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MembershipPackageInfo {
        private Long id;
        private String name;
        private String description;
        private Integer duration;
        private BigDecimal price;
    }
}