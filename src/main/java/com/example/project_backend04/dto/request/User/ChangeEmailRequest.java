package com.example.project_backend04.dto.request.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeEmailRequest {
    
    @NotBlank(message = "New email is required")
    @Email(message = "Invalid email format")
    private String newEmail;
}
