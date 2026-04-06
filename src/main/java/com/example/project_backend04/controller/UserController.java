package com.example.project_backend04.controller;

import com.example.project_backend04.dto.request.User.ChangePasswordRequest;
import com.example.project_backend04.dto.request.User.UpdateProfileRequest;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.dto.response.User.UserResponse;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.service.IService.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentProfile(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            UserResponse profile = userService.getUserProfile(user.getId());
            return ok(profile, "Profile retrieved successfully");
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }
    @PutMapping(value = "/profile", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        try {
            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            UserResponse updated = userService.updateProfile(user.getId(), request);
            return ok(updated, "Profile updated successfully");
        } catch (SecurityException e) {
            return forbidden(e.getMessage());
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    @PutMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> updateProfileWithAvatar(
            Authentication authentication,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar,
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
            @RequestParam(value = "dateOfBirth", required = false) String dateOfBirth,
            @RequestParam(value = "bio", required = false) String bio) {
        try {
            String currentEmail = authentication.getName();
            User user = userService.findByEmail(currentEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .fullName(fullName)
                    .email(email)
                    .phoneNumber(phoneNumber)
                    .dateOfBirth(dateOfBirth)
                    .bio(bio)
                    .build();
            
            UserResponse updated = userService.updateProfileWithAvatar(user.getId(), request, avatar);
            return ok(updated, "Profile and avatar updated successfully");
        } catch (SecurityException e) {
            return forbidden(e.getMessage());
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    /**
     * Change password
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        try {
            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            userService.changePassword(user.getId(), request);
            return ok(null, "Password changed successfully");
        } catch (SecurityException e) {
            return forbidden(e.getMessage());
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }


    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserStatus(
            @PathVariable Long id) {
        try {
            UserResponse updated = userService.toggleUserStatus(id);
            return ok(updated, "User status updated successfully");
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    // Helper methods
    private static <T> ResponseEntity<ApiResponse<T>> ok(T data, String msg) {
        return ResponseEntity.ok(new ApiResponse<>(true, msg, data, 200));
    }

    private static <T> ResponseEntity<ApiResponse<T>> badRequest(String msg) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, msg, null, 400));
    }

    private static <T> ResponseEntity<ApiResponse<T>> forbidden(String msg) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(false, msg, null, 403));
    }
}
