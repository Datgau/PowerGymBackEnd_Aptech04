package com.example.project_backend04.dto.response.Auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpStatusResponse {
    private boolean canResend;
    private long secondsUntilResend;
    private long sessionRemainingSeconds;
    private boolean sessionExpired;
    private String message;
}