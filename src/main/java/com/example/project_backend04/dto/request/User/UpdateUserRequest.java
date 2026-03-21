package com.example.project_backend04.dto.request.User;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private String password;
    
    @Email(message = "Email should be valid")
    private String email;
    
    private String avatar;
    private String fullName;
    private String phoneNumber;
    private Long roleId;
    private String dateOfBirth;
}
