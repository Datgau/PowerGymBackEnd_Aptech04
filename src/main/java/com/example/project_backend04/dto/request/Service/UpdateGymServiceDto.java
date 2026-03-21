package com.example.project_backend04.dto.request.Service;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdateGymServiceDto {
    private String name;
    private String description;
    private Long categoryId;
    private BigDecimal price;
    private Integer duration;
    private Integer maxParticipants;
    private List<MultipartFile> images;
    private List<String> deletedImages;
    private Boolean isActive;
}
