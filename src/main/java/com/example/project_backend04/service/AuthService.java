package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.Auth.OAuthLoginRequest;
import com.example.project_backend04.dto.response.Auth.*;
import com.example.project_backend04.entity.PendingUser;
import com.example.project_backend04.entity.Role;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.dto.request.Auth.LoginRequest;
import com.example.project_backend04.dto.request.Auth.RegisterRequest;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.entity.UserProvider;
import com.example.project_backend04.repository.AuthRepository;
import com.example.project_backend04.repository.PendingUserRepository;
import com.example.project_backend04.repository.UserProviderRepository;
import com.example.project_backend04.security.JwtService;
import com.example.project_backend04.service.IService.IAuthService;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
    public ApiResponse<RegisterResponse> register(RegisterRequest request) throws MessagingException {
        LocalDateTime now = LocalDateTime.now();
        if (authRepository.existsByEmail(request.getEmail())) {
            RegisterResponse data = new RegisterResponse(

                    request.getEmail(),
                    false,
                    now
            );
            return new ApiResponse<>(false, "Email đã tồn tại trong hệ thống", data, 400);
        }
        if (authRepository.existsByUsername(request.getUsername())) {
            RegisterResponse data = new RegisterResponse(
                    request.getUsername(),
                    false,
                    now
            );
            return new ApiResponse<>(false, "User name đã tồn tại trong hệ thống", data, 400);
        }
        Optional<PendingUser> pendingOptional = pendingUserRepository.findByEmail(request.getEmail());

        if (pendingOptional.isPresent()) {
            PendingUser existingPending = pendingOptional.get();
            if (existingPending.getOtpExpiry().isAfter(now)) {
                long secondsLeft = java.time.Duration.between(now, existingPending.getOtpExpiry()).getSeconds();
                RegisterResponse data = new RegisterResponse(
                        request.getEmail(),
                        false,
                        now
                );
                return new ApiResponse<>(false, "Email đã đăng ký, vui lòng thử lại sau " + secondsLeft + " giây", data, 400);
            } else {
                pendingUserRepository.delete(existingPending);
            }
        }

        PendingUser pendingUser = new PendingUser();
        pendingUser.setUsername(request.getUsername());
        pendingUser.setEmail(request.getEmail());
        pendingUser.setPassword(passwordEncoder.encode(request.getPassword()));

        String otp = generateOtp();
        pendingUser.setOtp(otp);
        pendingUser.setOtpExpiry(now.plusMinutes(5));

        pendingUserRepository.save(pendingUser);
        emailService.sendOtpEmail(request.getEmail(), otp);

        RegisterResponse data = new RegisterResponse(
                request.getEmail(),
                false,
                now
        );

        return new ApiResponse<>(true, "Đăng ký thành công, OTP đã được gửi tới email của bạn để xác minh tài khoản", data, 200);
    }

    @Transactional
    @Override
    public ApiResponse<Void> verifyOtp(String email, String otp) {
        Optional<PendingUser> optionalPending = pendingUserRepository.findByEmail(email);
        if (optionalPending.isEmpty()) {
            return new ApiResponse<>(false, "Email không tồn tại hoặc chưa đăng ký", null, 404);
        }

        PendingUser pendingUser = optionalPending.get();

        if (pendingUser.getOtpExpiry().isBefore(LocalDateTime.now())) {
            pendingUserRepository.delete(pendingUser);
            return new ApiResponse<>(false, "OTP đã hết hạn", null, 400);
        }

        if (!pendingUser.getOtp().equals(otp)) {
            return new ApiResponse<>(false, "OTP không đúng", null, 400);
        }


        User user = new User();
        user.setUsername(pendingUser.getUsername());
        user.setEmail(pendingUser.getEmail());
        user.setPassword(pendingUser.getPassword());

        Role defaultRole = authRepository.findRoleByName("USER")
                .orElseThrow(() -> new RuntimeException("Role USER chưa tồn tại"));
        user.setRole(defaultRole);

        authRepository.save(user);
        pendingUserRepository.delete(pendingUser);

        try {
            emailService.sendSuccessRegisterEmail(user.getEmail(), user.getUsername());
        } catch (MessagingException e) {
            System.err.println("Không thể gửi email xác nhận: " + e.getMessage());
            return new ApiResponse<>(false, "Không thể gửi email xác nhận: " + e.getMessage(), null, 400);
        }

        return new ApiResponse<>(true, "Xác minh email thành công!", null, 200);
    }


    @Transactional
    @Override
    public ApiResponse<LoginResponse> login(LoginRequest request, HttpServletResponse response) {

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            return new ApiResponse<>(false, "Sai tài khoản hoặc mật khẩu", null, 401);
        }

        User user = authRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

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
                user.getUsername(),
                user.getRole().getName(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatar(),
                jwtResponse
        );

        return new ApiResponse<>(true, "Đăng nhập thành công", loginResponse, 200);
    }


    @Override
    public ApiResponse<JwtResponse> refreshToken(String refreshToken, HttpServletResponse response) {

        User user = authRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token không hợp lệ"));

        if (user.getRefreshTokenExpiryTime().isBefore(LocalDateTime.now())) {
            return new ApiResponse<>(false, "Refresh token đã hết hạn", null, 401);
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

        return new ApiResponse<>(true, "Làm mới token thành công", jwtResponse, 200);
    }


    @Transactional
    @Override
    public ApiResponse<Void> logout(String refreshToken) {
        User user = authRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token không hợp lệ"));

        user.setRefreshToken(null);
        user.setRefreshTokenExpiryTime(null);
        authRepository.save(user);

        return new ApiResponse<>(true, "Đăng xuất thành công", null, 200);
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
        System.out.println("=== OAuth Login Request ===");
        System.out.println("Provider: " + request.getProvider());
        System.out.println("Token length: " + (request.getAccessToken() != null ? request.getAccessToken().length() : "null"));

        User user;

        switch (request.getProvider().toLowerCase()) {
            case "facebook":
                FacebookUserData fbUser = facebookApiService.getUserInfo(request.getAccessToken());
                if (fbUser == null || fbUser.getId() == null) {
                    System.out.println("Facebook user data is null or invalid");
                    return new ApiResponse<>(false, "AccessToken Facebook không hợp lệ", null, 401);
                }
                user = getOrCreateOAuthUser("facebook", fbUser.getId(), fbUser.getEmail(), fbUser.getName(), fbUser.getPictureUrl(), request.getAccessToken());
                break;

            case "google":
                GoogleUserData googleUser = googleApiService.getUserInfo(request.getAccessToken());
                System.out.println("Google user data: " + (googleUser != null ? "Valid" : "NULL"));
                if (googleUser != null) {
                    System.out.println("Google user ID: " + googleUser.getId());
                    System.out.println("Google user email: " + googleUser.getEmail());
                }
                if (googleUser == null || googleUser.getId() == null) {
                    System.out.println("Google user data is null or invalid");
                    return new ApiResponse<>(false, "AccessToken Google không hợp lệ", null, 401);
                }
                user = getOrCreateOAuthUser("google", googleUser.getId(), googleUser.getEmail(), googleUser.getFullName(), googleUser.getAvatar(), request.getAccessToken());
                break;

            default:
                return new ApiResponse<>(false, "Provider không hợp lệ", null, 400);
        }

        String jwtToken = jwtService.generateToken(user);
        String refreshToken = UUID.randomUUID().toString();

        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiryTime(LocalDateTime.now().plusDays(7));
        authRepository.save(user);

        jwtService.addRefreshTokenCookie(response, refreshToken);

        JwtResponse jwtResponse = new JwtResponse(jwtToken, jwtService.getValidDuration());
        LoginResponse loginResponse = new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getRole().getName(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatar(),
                jwtResponse
        );

        return new ApiResponse<>(true, "Đăng nhập OAuth thành công", loginResponse, 200);
    }

    /**
     * Tạo hoặc lấy user từ user_providers
     */
    @Transactional
    protected User getOrCreateOAuthUser(String provider, String providerUserId, String email, String fullName, String avatar, String accessToken) {
        Optional<UserProvider> optionalProvider =
                userProviderRepository.findByProviderAndProviderUserId(provider, providerUserId);

        if (optionalProvider.isPresent()) {
            return optionalProvider.get().getUser();
        }

        Optional<User> existingUserByEmail = authRepository.findByEmail(email);

        if (existingUserByEmail.isPresent()) {
            return existingUserByEmail.get();
        } else {
            User user = new User();
            user.setUsername(provider + "_" + providerUserId);
            user.setEmail(email);
            user.setFullName(fullName);
            user.setAvatar(avatar);

            user.setPassword(new BCryptPasswordEncoder().encode(UUID.randomUUID().toString()));
            Role defaultRole = authRepository.findRoleByName("USER")
                    .orElseThrow(() -> new RuntimeException("Role USER chưa tồn tại"));
            user.setRole(defaultRole);
            authRepository.save(user);

            UserProvider providerEntity = new UserProvider();
            providerEntity.setProvider(provider);
            providerEntity.setProviderUserId(providerUserId);
            providerEntity.setAccessToken(accessToken);
            providerEntity.setUser(user);
            userProviderRepository.save(providerEntity);

            return user;
        }
    }
}
