package com.example.project_backend04.controller;

import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.dto.response.User.UserResponse;
import com.example.project_backend04.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/{userId}/avatar")
    public ResponseEntity<ApiResponse<UserResponse>> uploadAvatar(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file) {

        ApiResponse<UserResponse> response = userService.uploadUserAvatar(userId, file);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

}
