package com.example.project_backend04.dto.request.User;

import lombok.Data;

@Data
public class CreateUserRequest {
    private String username;
    private String password;
    private String email;
    private String fullName;
    private String phoneNumber;
    private Long roleId;
}
