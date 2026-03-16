package com.example.project_backend04.dto.response.Trainer;

import com.example.project_backend04.entity.ServiceCategory;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TrainerResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String avatar;
    private String bio;
    private String coverPhoto;
    private Boolean isActive;
    private LocalDateTime createDate;
    
    // Thông tin trainer
    private Integer totalExperienceYears;
    private String education;
    private String emergencyContact;
    private String emergencyPhone;
    
    // Chuyên môn
    private List<TrainerSpecialtyResponse> specialties;
    
    // Giấy tờ
    private List<TrainerDocumentResponse> documents;
    
    @Data
    public static class TrainerSpecialtyResponse {
        private Long id;
        private ServiceCategoryResponse specialty;
        private String description;
        private Integer experienceYears;
        private String certifications;
        private String level;
        private Boolean isActive;
        private LocalDateTime createdAt;
    }
    
    @Data
    public static class ServiceCategoryResponse {
        private Long id;
        private String name;
        private String displayName;
        private String description;
        private String icon;
        private String color;
        private Boolean isActive;
        private Integer sortOrder;
    }
    
    @Data
    public static class TrainerDocumentResponse {
        private Long id;
        private String documentType;
        private String fileName;
        private String fileUrl;
        private String description;
        private LocalDateTime expiryDate;
        private Boolean isVerified;
        private Boolean isActive;
        private LocalDateTime createdAt;
    }
}