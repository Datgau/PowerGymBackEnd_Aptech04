package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.Auth.LoginRequest;
import com.example.project_backend04.dto.request.Auth.OAuthLoginRequest;
import com.example.project_backend04.dto.request.Auth.RegisterRequest;
import com.example.project_backend04.dto.response.Auth.*;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.entity.*;
import com.example.project_backend04.repository.AuthRepository;
import com.example.project_backend04.repository.PasswordResetTokenRepository;
import com.example.project_backend04.repository.PendingUserRepository;
import com.example.project_backend04.repository.UserProviderRepository;
import com.example.project_backend04.security.JwtService;
import com.example.project_backend04.service.IService.IAuthService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.MessagingException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private final AuthRepository authRepository;
    private final PendingUserRepository pendingUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserProviderRepository userProviderRepository;
    private final FacebookApiService facebookApiService;
    private final GoogleApiService googleApiService;

    @Value("${CORS_ALLOWED_ORIGINS}")
    private String frontendUrl;

    @Transactional
    @Override
    public ApiResponse<RegisterResponse> register(RegisterRequest request) {

        LocalDateTime now = LocalDateTime.now();
        
        if (authRepository.existsByEmail(request.getEmail())) {
            return new ApiResponse<>(
                    false,
                    "Email already exists in the system. Please login or use the forgot password feature.",
                    null,
                    400
            );
        }

        Optional<PendingUser> pendingOptional = pendingUserRepository.findByEmail(request.getEmail());

        String otp = generateOtp();
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        LocalDateTime otpExpiry = now.plusMinutes(1);

        if (pendingOptional.isPresent()) {
            PendingUser existingPending = pendingOptional.get();
            LocalDateTime tenMinutesAfterCreation = existingPending.getCreatedAt().plusMinutes(10);
            
            if (now.isAfter(tenMinutesAfterCreation)) {
                pendingUserRepository.delete(existingPending);
                
                PendingUser newPendingUser = new PendingUser();
                newPendingUser.setEmail(request.getEmail());
                newPendingUser.setPassword(encodedPassword);
                newPendingUser.setFullName(request.getFullName());
                newPendingUser.setOtp(otp);
                newPendingUser.setOtpExpiry(otpExpiry);
                
                try {
                    pendingUserRepository.save(newPendingUser);
                } catch (Exception e) {
                    return new ApiResponse<>(
                            false,
                            "Failed to process registration. Please try again.",
                            null,
                            500
                    );
                }
            } else {
                if (existingPending.getOtpExpiry().isAfter(now)) {
                    long secondsLeft = Duration.between(now, existingPending.getOtpExpiry()).getSeconds();
                    return new ApiResponse<>(
                            false,
                            "An OTP has already been sent to this email. Please wait " + secondsLeft + " seconds or use resend OTP.",
                            null,
                            400
                    );
                }
                
                // OTP expired but session valid - update with new OTP
                int updatedRows = pendingUserRepository.updatePendingUserByEmail(
                    request.getEmail(), 
                    encodedPassword, 
                    request.getFullName(), 
                    otp, 
                    otpExpiry
                );
                
                if (updatedRows == 0) {
                    return new ApiResponse<>(
                            false,
                            "Failed to update registration. Please try again.",
                            null,
                            500
                    );
                }
            }
        } else {
            PendingUser pendingUser = new PendingUser();
            pendingUser.setEmail(request.getEmail());
            pendingUser.setPassword(encodedPassword);
            pendingUser.setFullName(request.getFullName());
            pendingUser.setOtp(otp);
            pendingUser.setOtpExpiry(otpExpiry);

            try {
                pendingUserRepository.save(pendingUser);
            } catch (Exception e) {
                if (e.getMessage().contains("duplicate key") || e.getMessage().contains("unique constraint")) {
                    return new ApiResponse<>(
                            false,
                            "Registration is already in progress for this email. Please try again in a few moments.",
                            null,
                            409
                    );
                }
                throw e;
            }
        }

        try {
            emailService.sendOtpEmailAsync(request.getEmail(), otp);
        } catch (Exception e) {
            log.warn("Email service may be temporarily unavailable, but OTP has been created: {}", e.getMessage());
        }

        return new ApiResponse<>(
                true,
                "Registration successful. The OTP has been sent to your email.",
                null,
                200
        );
    }

    @Transactional
    @Override
    public ApiResponse<VerifyOtpResponse> verifyOtp(String email, String otp) {

        if (authRepository.existsByEmail(email)) {
            pendingUserRepository.findByEmail(email)
                    .ifPresent(pendingUserRepository::delete);
            return new ApiResponse<>(false, "Email has already been verified.", null, 400);
        }

        Optional<PendingUser> optionalPending = pendingUserRepository.findByEmail(email);
        if (optionalPending.isEmpty()) {
            return new ApiResponse<>(false, "Email does not exist or has not been registered.", null, 404);
        }

        PendingUser pendingUser = optionalPending.get();
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime tenMinutesAfterCreation = pendingUser.getCreatedAt().plusMinutes(10);
        if (now.isAfter(tenMinutesAfterCreation)) {
            pendingUserRepository.delete(pendingUser);
            return new ApiResponse<>(false, "Registration session has expired. Please register again.", null, 400);
        }

        if (pendingUser.getOtpExpiry().isBefore(now)) {
            return new ApiResponse<>(false, "OTP has expired. Please use resend OTP.", null, 400);
        }

        if (!pendingUser.getOtp().equals(otp)) {
            return new ApiResponse<>(false, "Invalid OTP.", null, 400);
        }
        User user = new User();
        user.setEmail(pendingUser.getEmail());
        user.setFullName(pendingUser.getFullName());
        user.setPassword(pendingUser.getPassword());

        Role defaultRole = authRepository.findRoleByName("USER")
                .orElseThrow(() -> new RuntimeException("Role USER does not exist."));

        user.setRole(defaultRole);
        user.setIsActive(true);
        authRepository.save(user);
        pendingUserRepository.delete(pendingUser);

        try {
            emailService.sendSuccessRegisterEmailAsync(user.getEmail(), user.getFullName());
        } catch (Exception e) {
            log.warn("Welcome email may not be sent: {}", e.getMessage());
        }

        return new ApiResponse<>(true, "Email verification successful!", null, 200);
    }

    @Transactional
    @Override
    public ApiResponse<RegisterResponse> resendOtp(String email) throws MessagingException {
        
        if (authRepository.existsByEmail(email)) {
            return new ApiResponse<>(
                    false,
                    "Email has already been verified and registered.",
                    null,
                    400
            );
        }

        Optional<PendingUser> optionalPending = pendingUserRepository.findByEmail(email);
        if (optionalPending.isEmpty()) {
            return new ApiResponse<>(
                    false,
                    "Email does not exist. Please register first.",
                    null,
                    404
            );
        }

        PendingUser pendingUser = optionalPending.get();
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime tenMinutesAfterCreation = pendingUser.getCreatedAt().plusMinutes(10);
        if (now.isAfter(tenMinutesAfterCreation)) {
            pendingUserRepository.delete(pendingUser);
            return new ApiResponse<>(
                    false,
                    "Registration session has expired. Please register again.",
                    null,
                    400
            );
        }

        if (pendingUser.getOtpExpiry().isAfter(now)) {
            long secondsLeft = Duration.between(now, pendingUser.getOtpExpiry()).getSeconds();
            return new ApiResponse<>(
                    false,
                    "Please wait " + secondsLeft + " seconds before requesting a new OTP.",
                    null,
                    400
            );
        }
        String newOtp = generateOtp();
        LocalDateTime newOtpExpiry = now.plusMinutes(1);

        int updatedRows = pendingUserRepository.updatePendingUserByEmail(
                email,
                pendingUser.getPassword(),
                pendingUser.getFullName(),
                newOtp,
                newOtpExpiry
        );

        if (updatedRows == 0) {
            return new ApiResponse<>(
                    false,
                    "Failed to resend OTP. Please try again.",
                    null,
                    500
            );
        }
        try {
            emailService.sendOtpEmailAsync(email, newOtp);
        } catch (Exception e) {
            log.warn("Email service may be temporarily unavailable for resend: {}", e.getMessage());
        }

        return new ApiResponse<>(
                true,
                "A new OTP has been sent to your email. Please verify within 10 minutes.",
                null,
                200
        );
    }

    @Override
    public ApiResponse<OtpStatusResponse> getOtpStatus(String email) {
        
        if (authRepository.existsByEmail(email)) {
            return new ApiResponse<>(
                    false,
                    "Email has already been verified and registered.",
                    OtpStatusResponse.builder()
                            .canResend(false)
                            .secondsUntilResend(0)
                            .sessionRemainingSeconds(0)
                            .sessionExpired(true)
                            .message("Email already registered")
                            .build(),
                    400
            );
        }

        Optional<PendingUser> optionalPending = pendingUserRepository.findByEmail(email);
        if (optionalPending.isEmpty()) {
            return new ApiResponse<>(
                    false,
                    "Email does not exist. Please register first.",
                    OtpStatusResponse.builder()
                            .canResend(false)
                            .secondsUntilResend(0)
                            .sessionRemainingSeconds(0)
                            .sessionExpired(true)
                            .message("Email not found")
                            .build(),
                    404
            );
        }

        PendingUser pendingUser = optionalPending.get();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tenMinutesAfterCreation = pendingUser.getCreatedAt().plusMinutes(10);
        long sessionRemainingSeconds = Duration.between(now, tenMinutesAfterCreation).getSeconds();
        
        if (now.isAfter(tenMinutesAfterCreation)) {
            return new ApiResponse<>(
                    false,
                    "Registration session has expired.",
                    OtpStatusResponse.builder()
                            .canResend(false)
                            .secondsUntilResend(0)
                            .sessionRemainingSeconds(0)
                            .sessionExpired(true)
                            .message("Session expired")
                            .build(),
                    400
            );
        }

        boolean canResend = pendingUser.getOtpExpiry().isBefore(now);
        long secondsUntilResend = 0;
        
        if (!canResend) {
            secondsUntilResend = Duration.between(now, pendingUser.getOtpExpiry()).getSeconds();
        }

        return new ApiResponse<>(
                true,
                "OTP status retrieved successfully.",
                OtpStatusResponse.builder()
                        .canResend(canResend)
                        .secondsUntilResend(Math.max(0, secondsUntilResend))
                        .sessionRemainingSeconds(Math.max(0, sessionRemainingSeconds))
                        .sessionExpired(false)
                        .message(canResend ? "Can resend OTP" : "Please wait before resending")
                        .build(),
                200
        );
    }



    @Transactional
    @Override
    public ApiResponse<LoginResponse> login(LoginRequest request, HttpServletResponse response) {

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            return new ApiResponse<>(
                    false,
                    "Invalid email or password.",
                    null,
                    401
            );
        }

        User user = authRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Authentication failed."));
        if (user.getIsActive() == null || !user.getIsActive()) {
            return new ApiResponse<>(
                    false,
                    "This account has been banned.",
                    null,
                    403
            );
        }
        String accessToken = jwtService.generateToken(user);
        String refreshToken = UUID.randomUUID().toString();

        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiryTime(LocalDateTime.now().plusDays(7));
        authRepository.save(user);

        jwtService.addRefreshTokenCookie(response, refreshToken);

        JwtResponse jwtResponse = new JwtResponse(
                accessToken,
                jwtService.getValidDuration()
        );

        LoginResponse loginResponse = new LoginResponse(
                user.getId(),
                user.getRole().getName(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatar(),
                jwtResponse
        );

        return new ApiResponse<>(
                true,
                "Login successful.",
                loginResponse,
                200
        );
    }


    @Transactional
    public ApiResponse<String> forgotPassword(String email) {

        User user = authRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return new ApiResponse<>(true, "If email exists, reset link sent.", null, 200);
        }
        if (user.getProviders() != null && !user.getProviders().isEmpty()) {
            return new ApiResponse<>(false, "Account uses social login.", null, 400);
        }

        String rawToken = UUID.randomUUID().toString();
        String tokenHash = passwordEncoder.encode(rawToken);
        PasswordResetToken token = PasswordResetToken.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .expiryTime(LocalDateTime.now().plusMinutes(15))
                .isUsed(false)
                .build();

        passwordResetTokenRepository.save(token);

        try {
            String resetLink = frontendUrl + "/reset-password?token=" + rawToken;
            emailService.sendResetPasswordEmail(user.getEmail(), resetLink);
        } catch (IOException e) {
            log.error("Failed to send reset password email: {}", e.getMessage());
        }
        return new ApiResponse<>(true, "If email exists, reset link sent.", null, 200);
    }

    @Transactional
    public ApiResponse<String> resetPassword(String token, String newPassword) {

        List<PasswordResetToken> tokens = passwordResetTokenRepository.findAllByIsUsedFalse();

        PasswordResetToken validToken = null;

        for (PasswordResetToken t : tokens) {
            if (passwordEncoder.matches(token, t.getTokenHash())) {
                validToken = t;
                break;
            }
        }

        if (validToken == null) {
            return new ApiResponse<>(false, "Invalid token.", null, 400);
        }

        if (validToken.getExpiryTime().isBefore(LocalDateTime.now())) {
            return new ApiResponse<>(false, "Token expired.", null, 400);
        }

        User user = authRepository.findById(validToken.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // update password
        user.setPassword(passwordEncoder.encode(newPassword));

        // invalidate refresh token (bảo mật)
        user.setRefreshToken(null);

        authRepository.save(user);

        // mark token used
        validToken.setIsUsed(true);
        passwordResetTokenRepository.save(validToken);

        return new ApiResponse<>(true, "Password reset successful.", null, 200);
    }


    @Override
    public ApiResponse<JwtResponse> refreshToken(String refreshToken, HttpServletResponse response) {

        User user = authRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token."));

        if (user.getRefreshTokenExpiryTime().isBefore(LocalDateTime.now())) {
            return new ApiResponse<>(
                    false,
                    "Refresh token has expired. Please log in again.",
                    null,
                    401
            );
        }
        if (user.getIsActive() == null || !user.getIsActive()) {
            return new ApiResponse<>(
                    false,
                    "This account has been banned.",
                    null,
                    403
            );
        }

        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = UUID.randomUUID().toString();

        user.setRefreshToken(newRefreshToken);
        user.setRefreshTokenExpiryTime(LocalDateTime.now().plusDays(7));
        authRepository.save(user);

        jwtService.addRefreshTokenCookie(response, newRefreshToken);

        JwtResponse jwtResponse = new JwtResponse(
                newAccessToken,
                jwtService.getValidDuration()
        );

        return new ApiResponse<>(
                true,
                "Token refreshed successfully.",
                jwtResponse,
                200
        );
    }


    @Transactional
    @Override
    public ApiResponse<Void> logout(String refreshToken) {

        User user = authRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token."));

        user.setRefreshToken(null);
        user.setRefreshTokenExpiryTime(null);
        authRepository.save(user);

        return new ApiResponse<>(
                true,
                "Logout successful.",
                null,
                200
        );
    }

    private String generateOtp() {
        int otp = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(otp);
    }

    @Scheduled(fixedRate = 60000)
    public void cleanExpiredPendingUsers() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tenMinutesAgo = now.minusMinutes(10);
        
        pendingUserRepository.deleteAllByCreatedAtBefore(tenMinutesAgo);
    }

//    ------------------------

    @Override
    @Transactional
    public ApiResponse<LoginResponse> oauthLogin(OAuthLoginRequest request, HttpServletResponse response) {

        User user;

        switch (request.getProvider().toLowerCase()) {

            case "facebook":
                FacebookUserData fbUser = facebookApiService.getUserInfo(request.getAccessToken());

                if (fbUser == null || fbUser.getId() == null) {
                    return new ApiResponse<>(
                            false,
                            "Invalid Facebook access token.",
                            null,
                            401
                    );
                }

                user = getOrCreateOAuthUser(
                        "facebook",
                        fbUser.getId(),
                        fbUser.getEmail(),
                        fbUser.getName(),
                        fbUser.getPictureUrl(),
                        request.getAccessToken()
                );
                break;

            case "google":
                GoogleUserData googleUser = googleApiService.getUserInfo(request.getAccessToken());

                if (googleUser == null || googleUser.getId() == null) {
                    return new ApiResponse<>(
                            false,
                            "Invalid Google access token.",
                            null,
                            401
                    );
                }

                user = getOrCreateOAuthUser(
                        "google",
                        googleUser.getId(),
                        googleUser.getEmail(),
                        googleUser.getFullName(),
                        googleUser.getAvatar(),
                        request.getAccessToken()
                );
                break;

            default:
                return new ApiResponse<>(
                        false,
                        "Unsupported OAuth provider.",
                        null,
                        400
                );
        }

        String jwtToken = jwtService.generateToken(user);
        String refreshToken = UUID.randomUUID().toString();

        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiryTime(LocalDateTime.now().plusDays(7));
        authRepository.save(user);

        jwtService.addRefreshTokenCookie(response, refreshToken);

        JwtResponse jwtResponse = new JwtResponse(
                jwtToken,
                jwtService.getValidDuration()
        );

        LoginResponse loginResponse = new LoginResponse(
                user.getId(),
                user.getRole().getName(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatar(),
                jwtResponse
        );

        return new ApiResponse<>(
                true,
                "OAuth login successful.",
                loginResponse,
                200
        );
    }
    @Transactional
    protected User getOrCreateOAuthUser(
            String provider,
            String providerUserId,
            String email,
            String fullName,
            String avatar,
            String accessToken
    ) {

        Optional<UserProvider> optionalProvider =
                userProviderRepository.findByProviderAndProviderUserId(provider, providerUserId);
        if (optionalProvider.isPresent()) {
            return optionalProvider.get().getUser();
        }
        Optional<User> existingUserByEmail = authRepository.findByEmail(email);
        User user;
        if (existingUserByEmail.isPresent()) {
            user = existingUserByEmail.get();
        } else {
            user = new User();
            user.setFullName(fullName != null ? fullName : provider + "_" + providerUserId);
            user.setEmail(email);
            user.setAvatar(avatar);

            user.setPassword(
                    new BCryptPasswordEncoder().encode(UUID.randomUUID().toString())
            );

            Role defaultRole = authRepository.findRoleByName("USER")
                    .orElseThrow(() -> new RuntimeException("Default role USER not found."));

            user.setRole(defaultRole);
            authRepository.save(user);
        }
        UserProvider providerEntity = new UserProvider();
        providerEntity.setProvider(provider);
        providerEntity.setProviderUserId(providerUserId);
        providerEntity.setAccessToken(accessToken);
        providerEntity.setUser(user);

        userProviderRepository.save(providerEntity);

        return user;
    }
}
