package com.example.project_backend04.dto.response.Auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JwtResponse {
    private String accessToken;
    private long expiresIn;
    /** Chỉ trả về khi gọi từ mobile (refresh-token-mobile endpoint). Web dùng HttpOnly cookie. */
    private String refreshToken;

    /** Constructor dùng cho web (không có refreshToken) */
    public JwtResponse(String accessToken, long expiresIn) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
    }
}