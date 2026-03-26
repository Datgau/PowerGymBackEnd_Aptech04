package com.example.project_backend04.controller;

import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.dto.response.User.UserResponse;
import com.example.project_backend04.service.UserService;
import com.example.project_backend04.service.IService.IAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final IAdminService adminService;

    @PostMapping("/{userId}/avatar")
    public ResponseEntity<ApiResponse<UserResponse>> uploadAvatar(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file) {

        ApiResponse<UserResponse> response = userService.uploadUserAvatar(userId, file);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserStatus(@PathVariable Long id) {
        ApiResponse<UserResponse> response = adminService.toggleUserStatus(id);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.NOT_FOUND)
                .body(response);
    }

}
