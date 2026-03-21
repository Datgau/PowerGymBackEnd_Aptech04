package com.example.project_backend04.dto.response.Trainer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor 
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


}