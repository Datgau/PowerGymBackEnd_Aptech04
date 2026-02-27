package com.example.project_backend04.dto.request.Auth;

import lombok.Data;

@Data
public class RefreshTokenRequest {
    private String refreshToken;
}
