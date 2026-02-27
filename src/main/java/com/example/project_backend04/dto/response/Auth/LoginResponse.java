package com.example.project_backend04.dto.response.Auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private Long id;
    private String role;
    private String email;
    private String fullName;
    private String avatar;
    private JwtResponse token;
}
