package com.example.project_backend04.controller;


import com.example.project_backend04.dto.request.Auth.*;
import com.example.project_backend04.dto.response.Auth.JwtResponse;
import com.example.project_backend04.dto.response.Auth.LoginResponse;
import com.example.project_backend04.dto.response.Auth.RegisterResponse;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.security.JwtService;
import com.example.project_backend04.service.IService.IAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessagingException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            ApiResponse<RegisterResponse> response = authService.register(request);
            return ResponseEntity
                    .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                    .body(response);
        } catch (MessagingException e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error sending verification email", null, 500));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Registration failed: " + e.getMessage(), null, 500));
        }
}


    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<?>> verifyOtp(@RequestBody VerifyOtpRequest request) {
        ApiResponse<?> response = authService.verifyOtp(request.getEmail(), request.getOtp());
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<RegisterResponse>> resendOtp(@RequestBody ResendOtpRequest request) {
        try {
            ApiResponse<RegisterResponse> response = authService.resendOtp(request.getEmail());
            return ResponseEntity
                    .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                    .body(response);
        } catch (MessagingException e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error sending OTP email", null, 500));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to resend OTP: " + e.getMessage(), null, 500));
        }
    }

    @PostMapping("/otp-status")
    public ResponseEntity<ApiResponse<com.example.project_backend04.dto.response.Auth.OtpStatusResponse>> getOtpStatus(@RequestBody OtpStatusRequest request) {
        try {
            ApiResponse<com.example.project_backend04.dto.response.Auth.OtpStatusResponse> response = authService.getOtpStatus(request.getEmail());
            return ResponseEntity
                    .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                    .body(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error retrieving OTP status: " + e.getMessage(), null, 500));
        }
    }
    @PostMapping("/forgot-password")
    public ApiResponse<String> forgotPassword(@RequestParam String email) {
        return authService.forgotPassword(email);
    }

    @PostMapping("/reset-password")
    public ApiResponse<String> resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword
    ) {
        return authService.resetPassword(token, newPassword);
    }
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        try {
            ApiResponse<LoginResponse> apiResponse = authService.login(request, response);

            return ResponseEntity
                    .status(apiResponse.isSuccess() ? HttpStatus.OK : HttpStatus.UNAUTHORIZED)
                    .body(apiResponse);
        } catch (BadCredentialsException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Login failed: " + e.getMessage()));
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<JwtResponse>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            String refreshToken = jwtService.getRefreshTokenFromCookie(request);

            ApiResponse<JwtResponse> apiResponse = authService.refreshToken(refreshToken, response);

            return ResponseEntity
                    .status(apiResponse.isSuccess() ? HttpStatus.OK : HttpStatus.UNAUTHORIZED)
                    .body(apiResponse);

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Token refresh failed: " + e.getMessage()));
        }
    }

    /**
     * Mobile-friendly refresh token endpoint — nhận refreshToken qua request body
     * thay vì HttpOnly cookie (cookie không hoạt động tốt trên mobile HTTP client).
     */
    @PostMapping("/refresh-token-mobile")
    public ResponseEntity<ApiResponse<JwtResponse>> refreshTokenMobile(
            @RequestBody RefreshTokenRequest request,
            HttpServletResponse response
    ) {
        try {
            if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, "Refresh token is required"));
            }
            // Cast to AuthService to access mobile-specific method
            ApiResponse<JwtResponse> apiResponse =
                    ((com.example.project_backend04.service.AuthService) authService)
                            .refreshTokenForMobile(request.getRefreshToken(), response);
            return ResponseEntity
                    .status(apiResponse.isSuccess() ? HttpStatus.OK : HttpStatus.UNAUTHORIZED)
                    .body(apiResponse);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Token refresh failed: " + e.getMessage()));
        }
    }

    @PostMapping("/oauth/login")
    public ResponseEntity<ApiResponse<LoginResponse>> oauthLogin(
            @RequestBody OAuthLoginRequest request,
            HttpServletResponse response
    ) {
        ApiResponse<LoginResponse> res = authService.oauthLogin(request, response);
        return ResponseEntity.status(res.isSuccess() ? 200 : 401).body(res);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout(
            @RequestBody RefreshTokenRequest request,
            HttpServletResponse response) {

        try {
            ApiResponse<?> result = authService.logout(request.getRefreshToken());

            jwtService.clearRefreshTokenCookie(response);

            return ResponseEntity
                    .status(result.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                    .body(result);

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Logout failed: " + e.getMessage()));
        }
    }

}
