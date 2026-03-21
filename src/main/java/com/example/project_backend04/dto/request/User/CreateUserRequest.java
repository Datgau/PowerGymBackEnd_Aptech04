package com.example.project_backend04.dto.request.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateUserRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    @NotBlank(message = "Full name is required")
    private String fullName;
    
    private String phoneNumber;
    
    @NotNull(message = "Role is required")
    private Long roleId;
    
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$|^$", message = "Date of birth must be in YYYY-MM-DD format")
    private String dateOfBirth;

    @Override
    public String toString() {
        return "CreateUserRequest{" +
                "email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", roleId=" + roleId +
                ", dateOfBirth='" + dateOfBirth + '\'' +
                '}';
    }
}
