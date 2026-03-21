package com.example.project_backend04.dto.response.TrainerBooking;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public  class SpecialtyInfo {
    private Long specialtyId;
    private String specialtyName;
    private String description;
    private Integer experienceYears;
    private String level;
    private String certifications;
}