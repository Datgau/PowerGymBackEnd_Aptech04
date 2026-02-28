package com.example.project_backend04.service.IService;

import com.example.project_backend04.dto.request.Auth.LoginRequest;
import com.example.project_backend04.dto.request.Auth.OAuthLoginRequest;
import com.example.project_backend04.dto.request.Auth.RegisterRequest;
import com.example.project_backend04.dto.response.Auth.*;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;

@Service
public interface IAuthService {
    ApiResponse<RegisterResponse> register(RegisterRequest request) throws MessagingException;
    ApiResponse<VerifyOtpResponse> verifyOtp(String email, String otp);
    ApiResponse<LoginResponse> login(LoginRequest request, HttpServletResponse response);
    ApiResponse<JwtResponse> refreshToken(String refreshToken, HttpServletResponse response);
    ApiResponse<Void> logout(String username);
    ApiResponse<LoginResponse> oauthLogin(OAuthLoginRequest request, HttpServletResponse response);
}
