package com.example.project_backend04.dto.request.Service;

import com.example.project_backend04.enums.ServiceCategory;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Data
public class GymServiceRequest {

    private String name;
    private String description;
    private ServiceCategory category;
    private BigDecimal price;
    private Integer duration;
    private Integer maxParticipants;
    private List<MultipartFile> images;
    private Boolean isActive;
}