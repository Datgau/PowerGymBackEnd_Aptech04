package com.example.project_backend04.dto.response.Trainer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO trả về thông tin trainer cho màn hình chọn trainer khi đặt lịch.
 * Khớp với TrainerSpecialtyItem trong frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainerForBookingResponse {

    private Long id;
    private String fullName;
    private String email;
    private String avatar;
    private String bio;
    private Integer totalExperienceYears;
    private Boolean isActive;
    private List<SpecialtyInfo> specialties;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecialtyInfo {
        private Long id;
        private CategoryInfo specialty;
        private Integer experienceYears;
        private String level;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryInfo {
        private Long id;
        private String name;
        private String displayName;
    }
}
