package com.example.project_backend04.dto.request.Auth;

import lombok.Data;


@Data
public class OAuthLoginRequest {
    private String provider;
    private String accessToken;
}
