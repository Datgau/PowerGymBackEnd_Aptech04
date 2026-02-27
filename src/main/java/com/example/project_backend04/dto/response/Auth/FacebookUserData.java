package com.example.project_backend04.dto.response.Auth;

import lombok.Data;

@Data
public class FacebookUserData {
    private String id;
    private String name;
    private String email;
    private String pictureUrl;
    private String accessToken;
}
