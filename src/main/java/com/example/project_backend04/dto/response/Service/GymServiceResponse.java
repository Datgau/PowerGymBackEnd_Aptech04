package com.example.project_backend04.dto.response.Service;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class GymServiceResponse {
    private Long id;
    private String name;
    private String description;
    private ServiceCategoryDto category;
    private List<String> images;
    private BigDecimal price;
    private Integer duration;
    private Integer maxParticipants;
    private Boolean isActive;
    private Long registrationCount; // Số lượng người đăng ký
    
    @Data
    public static class ServiceCategoryDto {
        private Long id;
        private String name;
        private String displayName;
        private String description;
        private String icon;
        private String color;
        private Boolean isActive;
        private Integer sortOrder;
    }
}
