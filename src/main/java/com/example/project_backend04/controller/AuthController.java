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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
                    .body(new ApiResponse<>(false, "Lỗi khi gửi email xác minh", null, 500));
        } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Đăng ký thất bại: " + e.getMessage(), null, 500));
    }

}


    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<?>> verifyOtp(@RequestBody VerifyOtpRequest request) {
        ApiResponse<?> response = authService.verifyOtp(request.getEmail(), request.getOtp());
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
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
                    .body(new ApiResponse<>(false, "Đăng nhập thất bại: " + e.getMessage()));
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
                    .body(new ApiResponse<>(false, "Làm mới token thất bại: " + e.getMessage()));
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
                    .body(new ApiResponse<>(false, "Đăng xuất thất bại: " + e.getMessage()));
        }
    }

}
