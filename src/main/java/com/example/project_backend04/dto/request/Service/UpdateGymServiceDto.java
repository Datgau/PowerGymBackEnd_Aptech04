package com.example.project_backend04.dto.request.Service;

import com.example.project_backend04.enums.ServiceCategory;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Data
public class UpdateGymServiceDto {
    private String name;
    private String description;
    private ServiceCategory category;
    private BigDecimal price;
    private Integer duration;
    private Integer maxParticipants;
    private MultipartFile image;
    private Boolean isActive;
}
