package com.example.project_backend04.dto.response.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerSpecialtyResponse {
    private Long id;
    private SpecialtyInfo specialty;
    private String description;
    private Integer experienceYears;
    private String certifications;
    private String level;
    private Boolean isActive;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SpecialtyInfo {
        private Long id;
        private String name;
        private String displayName;
    }
}