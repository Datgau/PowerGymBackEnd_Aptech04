package com.example.project_backend04.dto.request.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {
    
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;
    
    @Email(message = "Invalid email format")
    private String email;
    
    @Size(max = 15, message = "Phone number must not exceed 15 characters")
    private String phoneNumber;
    
    private String dateOfBirth; // Format: yyyy-MM-dd
    
    @Size(max = 500, message = "Bio must not exceed 500 characters")
    private String bio;
    
    private String avatar; // URL or base64
}
