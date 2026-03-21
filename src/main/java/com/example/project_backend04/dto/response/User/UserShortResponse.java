package com.example.project_backend04.dto.response.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserShortResponse {
    private Long id;
    private String email;
    private String fullName;
    private String username;
}