package com.example.project_backend04.dto.response.Auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {
    private Long id;
    private String role;
    private String email;
    private String fullName;
    private String avatar;
    private JwtResponse token;
    private String phoneNumber;
    private String bio;
    private String dateOfBirth;
    /**
     * Refresh token trả về trong body để mobile client lưu vào local storage.
     * Web client dùng HttpOnly cookie thay thế.
     */
    private String refreshToken;
}
