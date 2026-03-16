package com.example.project_backend04.dto.response.ServiceCategory;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ServiceCategoryResponse {
    private Long id;
    private String name;
    private String displayName;
    private String description;
    private String icon;
    private String color;
    private Boolean isActive;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Statistics
    private Long trainerCount; // Số trainers sử dụng category này
    private Long serviceCount; // Số gym services sử dụng category này
}