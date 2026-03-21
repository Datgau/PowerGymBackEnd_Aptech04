package com.example.project_backend04.dto.response.Trainer;

import com.example.project_backend04.dto.response.ServiceCategory.ServiceCategoryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerSpecialtyResponse {
    
    private Long id;
    private ServiceCategoryResponse specialty;
    private String description;
    private Integer experienceYears;
    private String level;
    private String certifications;
    private Boolean isActive;
    private LocalDateTime createdAt;
    
    public boolean hasExperience() {
        return experienceYears != null && experienceYears > 0;
    }
    
    public boolean hasCertifications() {
        return certifications != null && !certifications.trim().isEmpty();
    }
    
    public String getFormattedExperience() {
        if (hasExperience()) {
            return experienceYears + " year" + (experienceYears > 1 ? "s" : "") + " experience";
        }
        return "New to this specialty";
    }
    
    public String getLevelDisplay() {
        if (level == null || level.trim().isEmpty()) {
            return "Not specified";
        }
        return level.substring(0, 1).toUpperCase() + level.substring(1).toLowerCase();
    }
}