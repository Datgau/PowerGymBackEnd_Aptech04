package com.example.project_backend04.controller;

import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.security.CustomUserDetails;
import com.example.project_backend04.service.IService.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{userId}/follow")
    public ApiResponse<Void> follow(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return followService.follow(user.getId(), userId);
    }

    @DeleteMapping("/{userId}/unfollow")
    public ApiResponse<Void> unfollow(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return followService.unfollow(user.getId(), userId);
    }

    @GetMapping("/{userId}/is-following")
    public ApiResponse<Boolean> isFollowing(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return followService.isFollowing(user.getId(), userId);
    }

    @GetMapping("/{userId}/is-mutual")
    public ApiResponse<Boolean> isMutual(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return followService.isMutualFollow(user.getId(), userId);
    }
}
