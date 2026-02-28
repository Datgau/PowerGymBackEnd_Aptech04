package com.example.project_backend04.dto.request.User;

import lombok.Data;

@Data
public class CreateUserRequest {
    private String email;
    private String fullName;
    private String phoneNumber;
    private Long roleId;
}
