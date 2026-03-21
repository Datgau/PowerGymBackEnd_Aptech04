package com.example.project_backend04.dto.response.User;

import com.example.project_backend04.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDetailResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String dateOfBirth;
    private String avatar;
    private String bio;
    private String coverPhoto;
    private LocalDateTime createDate;
    private Role role;
    private Boolean isActive;
    
    // Trainer-specific fields
    private Integer totalExperienceYears;
    private String education;
    private String emergencyContact;
    private String emergencyPhone;
}