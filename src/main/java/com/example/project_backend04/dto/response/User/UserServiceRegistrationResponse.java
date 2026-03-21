package com.example.project_backend04.dto.response.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserServiceRegistrationResponse {
    private Long id;
    private ServiceInfo service;
    private LocalDateTime registrationDate;
    private LocalDateTime expirationDate;
    private String status;
    private String notes;
    private TrainerInfo trainer;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServiceInfo {
        private Long id;
        private String name;
        private String description;
        private BigDecimal price;
        private CategoryInfo category;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryInfo {
        private Long id;
        private String name;
        private String displayName;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrainerInfo {
        private Long id;
        private String fullName;
        private String avatar;
    }
}