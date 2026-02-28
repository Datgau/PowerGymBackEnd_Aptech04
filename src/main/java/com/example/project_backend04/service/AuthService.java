package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.Auth.LoginRequest;
import com.example.project_backend04.dto.request.Auth.OAuthLoginRequest;
import com.example.project_backend04.dto.request.Auth.RegisterRequest;
import com.example.project_backend04.dto.response.Auth.*;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.entity.PendingUser;
import com.example.project_backend04.entity.Role;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.entity.UserProvider;
import com.example.project_backend04.repository.AuthRepository;
import com.example.project_backend04.repository.PendingUserRepository;
import com.example.project_backend04.repository.UserProviderRepository;
import com.example.project_backend04.security.JwtService;
import com.example.project_backend04.service.IService.IAuthService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.MessagingException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private final AuthRepository authRepository;
    private final PendingUserRepository pendingUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final UserProviderRepository userProviderRepository;
    private final FacebookApiService facebookApiService;
    private final GoogleApiService googleApiService;

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

        if (pendingOptional.isPresent()) {
            PendingUser existingPending = pendingOptional.get();
            if (existingPending.getOtpExpiry().isAfter(now)) {
                long secondsLeft = Duration.between(now, existingPending.getOtpExpiry()).getSeconds();

                return new ApiResponse<>(
                        false,
                        "An OTP has already been sent to this email. Please wait " + secondsLeft + " seconds.",
                        null,
                        400
                );
            }
            pendingUserRepository.delete(existingPending);
        }

        PendingUser pendingUser = new PendingUser();
        pendingUser.setEmail(request.getEmail());
        pendingUser.setPassword(passwordEncoder.encode(request.getPassword()));
        pendingUser.setFullName(request.getFullName());

        String otp = generateOtp();
        pendingUser.setOtp(otp);
        pendingUser.setOtpExpiry(now.plusMinutes(5));

        pendingUserRepository.save(pendingUser);

        try {
            emailService.sendOtpEmail(request.getEmail(), otp);
        } catch (Exception e) {
            e.printStackTrace();
            pendingUserRepository.delete(pendingUser);
            return new ApiResponse<>(false, "Unable to send OTP email.", null, 500);
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

        if (pendingUser.getOtpExpiry().isBefore(now)) {
            pendingUserRepository.delete(pendingUser);
            return new ApiResponse<>(false, "OTP has expired.", null, 400);
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
        authRepository.save(user);
        pendingUserRepository.delete(pendingUser);

        try {
            emailService.sendSuccessRegisterEmail(user.getEmail(), user.getFullName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ApiResponse<>(true, "Email verification successful!", null, 200);
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
        pendingUserRepository.deleteAllByOtpExpiryBefore(now);
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
