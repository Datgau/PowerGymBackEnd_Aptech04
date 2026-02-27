package com.example.project_backend04.dto.response.Auth;

import lombok.Data;

@Data
public class GoogleUserData {
    private String id;
    private String fullName;
    private String email;
    private String avatar;
}