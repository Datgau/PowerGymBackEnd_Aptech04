package com.example.project_backend04.service.IService;

public interface ILikeService {
    boolean toggleLike(Long postId, Long userId);
    int getLikeCount(Long postId);
    boolean isLikedByUser(Long postId, Long userId);
}
