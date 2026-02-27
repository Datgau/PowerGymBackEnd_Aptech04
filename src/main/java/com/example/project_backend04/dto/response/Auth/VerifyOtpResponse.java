package com.example.project_backend04.dto.response.Auth;

import java.time.LocalDateTime;

public record VerifyOtpResponse(
        String email,
        boolean verified,
        LocalDateTime verifiedAt
) {}
