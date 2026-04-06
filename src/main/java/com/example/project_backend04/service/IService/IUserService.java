package com.example.project_backend04.service.IService;

import com.example.project_backend04.dto.request.User.ChangePasswordRequest;
import com.example.project_backend04.dto.request.User.UpdateProfileRequest;
import com.example.project_backend04.dto.response.User.UserResponse;
import com.example.project_backend04.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public interface IUserService {
    
    /**
     * Get user profile by ID
     */
    UserResponse getUserProfile(Long userId);
    
    /**
     * Update user profile information
     */
    UserResponse updateProfile(Long userId, UpdateProfileRequest request);
    
    /**
     * Update user profile with avatar upload
     */
    UserResponse updateProfileWithAvatar(Long userId, UpdateProfileRequest request, MultipartFile avatar);
    
    /**
     * Change user password
     */
    void changePassword(Long userId, ChangePasswordRequest request);
    
    /**
     * Toggle user active status (Admin only)
     */
    UserResponse toggleUserStatus(Long userId);
    
    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);
}
