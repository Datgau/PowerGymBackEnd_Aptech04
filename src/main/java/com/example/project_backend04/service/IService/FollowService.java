package com.example.project_backend04.service.IService;

import com.example.project_backend04.dto.response.Shared.ApiResponse;

public interface FollowService {
    ApiResponse<Boolean> isFollowing(Long followerId, Long followingId);
    ApiResponse<Boolean> isMutualFollow(Long userA, Long userB);
    ApiResponse<Void> follow(Long followerId, Long followingId);
    ApiResponse<Void> unfollow(Long followerId, Long followingId);
}
