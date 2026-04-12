package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.User.ChangePasswordRequest;
import com.example.project_backend04.dto.request.User.UpdateProfileRequest;
import com.example.project_backend04.dto.response.User.UserResponse;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.mapper.UserMapper;
import com.example.project_backend04.repository.UserRepository;
import com.example.project_backend04.service.IService.ICloudinaryService;
import com.example.project_backend04.service.IService.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ICloudinaryService cloudinaryService;
    private final UserMapper userMapper;
    private final OtpService otpService;
    private final EmailService emailService;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // Update fields if provided
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            // Check if email is already taken by another user
            if (!request.getEmail().equals(user.getEmail())) {
                userRepository.findByEmail(request.getEmail()).ifPresent(existingUser -> {
                    if (!existingUser.getId().equals(userId)) {
                        throw new IllegalArgumentException("Email is already taken");
                    }
                });
                user.setEmail(request.getEmail());
            }
        }

        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }

        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        if (request.getAvatar() != null && !request.getAvatar().isBlank()) {
            user.setAvatar(request.getAvatar());
        }

        User savedUser = userRepository.save(user);
        log.info("User profile updated successfully for user ID: {}", userId);
        return userMapper.toResponse(savedUser);
    }

    @Override
    public UserResponse updateProfileWithAvatar(Long userId, UpdateProfileRequest request, MultipartFile avatar) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        if (avatar != null && !avatar.isEmpty()) {
            try {
                String avatarUrl = cloudinaryService.uploadSingleFile(avatar, "user_avatars");
                user.setAvatar(avatarUrl);
                log.info("Avatar uploaded successfully for user ID: {}", userId);
            } catch (Exception e) {
                log.error("Failed to upload avatar for user ID: {}", userId, e);
                throw new RuntimeException("Failed to upload avatar: " + e.getMessage());
            }
        }
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (!request.getEmail().equals(user.getEmail())) {
                userRepository.findByEmail(request.getEmail()).ifPresent(existingUser -> {
                    if (!existingUser.getId().equals(userId)) {
                        throw new IllegalArgumentException("Email is already taken");
                    }
                });
                user.setEmail(request.getEmail());
            }
        }

        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }

        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        User savedUser = userRepository.save(user);
        log.info("User profile with avatar updated successfully for user ID: {}", userId);
        return userMapper.toResponse(savedUser);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new SecurityException("Current password is incorrect");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public void sendEmailChangeOtp(Long userId, String newEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException("New email must be different from current email");
        }
        userRepository.findByEmail(newEmail).ifPresent(existingUser -> {
            throw new IllegalArgumentException("Email is already taken");
        });
        String currentEmailOtp = otpService.generateOtp();
        String currentEmailKey = "email_change:current:" + user.getEmail().toLowerCase();
        otpService.storeOtp(currentEmailKey, currentEmailOtp);
        otpService.storeOtp(currentEmailKey + ":newEmail", newEmail);
        try {
            emailService.sendOtpEmail(user.getEmail(), currentEmailOtp, user.getFullName());
        } catch (Exception e) {
            otpService.clearOtp(currentEmailKey);
            otpService.clearOtp(currentEmailKey + ":newEmail");
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage());
        }
    }

    @Override
    public void verifyCurrentEmailOtp(Long userId, String otp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        String currentEmailKey = "email_change:current:" + user.getEmail().toLowerCase();
        if (!otpService.verifyOtp(currentEmailKey, otp)) {
            throw new SecurityException("Invalid or expired OTP for current email");
        }
        String newEmail = otpService.getOtp(currentEmailKey + ":newEmail");
        if (newEmail == null) {
            throw new IllegalArgumentException("Email change request not found or expired");
        }
        String newEmailOtp = otpService.generateOtp();
        String newEmailKey = "email_change:new:" + newEmail.toLowerCase();
        otpService.storeOtp(newEmailKey, newEmailOtp);
        otpService.storeOtp(newEmailKey + ":userId", String.valueOf(userId));
        otpService.storeOtp(newEmailKey + ":currentEmail", user.getEmail());

        try {
            emailService.sendOtpEmail(newEmail, newEmailOtp, user.getFullName());
        } catch (Exception e) {
            otpService.clearOtp(newEmailKey);
            otpService.clearOtp(newEmailKey + ":userId");
            otpService.clearOtp(newEmailKey + ":currentEmail");
            throw new RuntimeException("Failed to send OTP to new email: " + e.getMessage());
        }
    }

    @Override
    public UserResponse verifyNewEmailOtpAndChangeEmail(Long userId, String newEmail, String otp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        String newEmailKey = "email_change:new:" + newEmail.toLowerCase();
        if (!otpService.verifyOtp(newEmailKey, otp)) {
            throw new SecurityException("Invalid or expired OTP for new email");
        }
        String storedUserId = otpService.getOtp(newEmailKey + ":userId");
        if (storedUserId == null || !storedUserId.equals(String.valueOf(userId))) {
            throw new SecurityException("Invalid email change request");
        }
        userRepository.findByEmail(newEmail).ifPresent(existingUser -> {
            throw new IllegalArgumentException("Email is already taken");
        });
        String oldEmail = user.getEmail();
        user.setEmail(newEmail);
        User savedUser = userRepository.save(user);
        String currentEmailKey = "email_change:current:" + oldEmail.toLowerCase();
        otpService.clearOtp(currentEmailKey);
        otpService.clearOtp(currentEmailKey + ":newEmail");
        otpService.clearOtp(newEmailKey);
        otpService.clearOtp(newEmailKey + ":userId");
        otpService.clearOtp(newEmailKey + ":currentEmail");
        return userMapper.toResponse(savedUser);
    }

    @Override
    public UserResponse toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        user.setIsActive(!user.getIsActive());
        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
