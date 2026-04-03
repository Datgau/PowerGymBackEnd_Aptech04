package com.example.project_backend04.dto.response.Trainer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableTrainerResponse {
    private Long id;
    private String fullName;
    private String avatar;
    private List<String> specialtyNames;
    private Integer totalExperienceYears;
}
