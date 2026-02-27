package com.example.project_backend04.dto.response.Service;

import com.example.project_backend04.enums.ServiceCategory;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class GymServiceResponse {
    private Long id;
    private String name;
    private String description;
    private ServiceCategory category;
    private List<String> images;
    private BigDecimal price;
    private Integer duration;
    private Integer maxParticipants;
    private Boolean isActive;
}
