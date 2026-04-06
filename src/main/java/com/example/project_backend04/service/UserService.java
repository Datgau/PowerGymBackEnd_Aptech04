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

        // Upload avatar if provided
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

        // Update other fields
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

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new SecurityException("Current password is incorrect");
        }

        // Verify new password and confirm password match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        // Check if new password is different from current password
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed successfully for user ID: {}", userId);
    }

    @Override
    public UserResponse toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // Toggle active status
        user.setIsActive(!user.getIsActive());
        User savedUser = userRepository.save(user);
        
        log.info("User status toggled for user ID: {}. New status: {}", userId, savedUser.getIsActive());
        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
