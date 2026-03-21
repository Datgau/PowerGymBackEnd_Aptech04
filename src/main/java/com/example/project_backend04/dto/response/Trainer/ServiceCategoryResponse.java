package com.example.project_backend04.dto.response.Trainer;

import lombok.Data;

@Data
public  class ServiceCategoryResponse {
    private Long id;
    private String name;
    private String displayName;
    private String description;
    private String icon;
    private String color;
    private Boolean isActive;
    private Integer sortOrder;
}