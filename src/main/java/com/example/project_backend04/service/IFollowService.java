package com.example.project_backend04.service;

import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.entity.Follow;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.repository.FollowRepository;
import com.example.project_backend04.repository.UserRepository;
import com.example.project_backend04.service.IService.FollowService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IFollowService implements FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public ApiResponse<Boolean> isFollowing(Long followerId, Long followingId) {
        boolean result = followRepository
                .existsByFollower_IdAndFollowing_Id(followerId, followingId);

        return new ApiResponse<>(
                true,
                result ? "Đang follow" : "Chưa follow",
                result,
                HttpStatus.OK.value()
        );
    }

    /**
     * Kiểm tra 2 người có follow nhau không
     */
    public ApiResponse<Boolean> isMutualFollow(Long userA, Long userB) {
        boolean aFollowB = followRepository
                .existsByFollower_IdAndFollowing_Id(userA, userB);

        boolean bFollowA = followRepository
                .existsByFollower_IdAndFollowing_Id(userB, userA);

        boolean mutual = aFollowB && bFollowA;

        return new ApiResponse<>(
                true,
                mutual ? "Hai người follow nhau" : "Chưa follow nhau",
                mutual,
                HttpStatus.OK.value()
        );
    }

    /**
     * Follow người khác
     */
    @Transactional
    public ApiResponse<Void> follow(Long followerId, Long followingId) {

        if (followerId.equals(followingId)) {
            return new ApiResponse<>(
                    false,
                    "Không thể follow chính mình",
                    null,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        if (followRepository.existsByFollower_IdAndFollowing_Id(followerId, followingId)) {
            return new ApiResponse<>(
                    false,
                    "Đã follow người này rồi",
                    null,
                    HttpStatus.CONFLICT.value()
            );
        }

        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new RuntimeException("Follower không tồn tại"));

        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new RuntimeException("User được follow không tồn tại"));

        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowing(following);

        followRepository.save(follow);

        return new ApiResponse<>(
                true,
                "Follow thành công",
                null,
                HttpStatus.CREATED.value()
        );
    }

    /**
     * Bỏ follow
     */
    @Transactional
    public ApiResponse<Void> unfollow(Long followerId, Long followingId) {

        if (!followRepository.existsByFollower_IdAndFollowing_Id(followerId, followingId)) {
            return new ApiResponse<>(
                    false,
                    "Chưa follow nên không thể unfollow",
                    null,
                    HttpStatus.NOT_FOUND.value()
            );
        }

        followRepository.deleteByFollower_IdAndFollowing_Id(followerId, followingId);

        return new ApiResponse<>(
                true,
                "Bỏ follow thành công",
                null,
                HttpStatus.OK.value()
        );
    }
}
