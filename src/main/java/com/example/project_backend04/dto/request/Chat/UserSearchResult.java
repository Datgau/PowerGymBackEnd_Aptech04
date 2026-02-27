package com.example.project_backend04.dto.request.Chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResult {
    private Long id;
    private String username;
    private String fullName;
    private String avatar;
}
