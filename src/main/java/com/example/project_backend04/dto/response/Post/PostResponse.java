package com.example.project_backend04.dto.response.Post;

import com.example.project_backend04.entity.Post;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostResponse {
    private Long id;
    private UserInfo user;
    private String content;
    private List<String> images;  // Danh sách URLs ảnh
    private int likesCount;
    private int commentsCount;
    private LocalDateTime createdAt;
    private boolean isLikedByCurrentUser;

    @Data
    public static class UserInfo {
        private Long id;
        private String username;
        private String fullName;
        private String avatar;
    }

    /**
     * Convert từ Post entity sang PostResponse DTO
     */
    public static PostResponse fromEntity(Post post, Long currentUserId) {
        PostResponse response = new PostResponse();
        response.setId(post.getId());
        
        // User info
        UserInfo userInfo = new UserInfo();
        userInfo.setId(post.getUser().getId());
        userInfo.setUsername(post.getUser().getUsername());
        userInfo.setFullName(post.getUser().getFullName());
        userInfo.setAvatar(post.getUser().getAvatar());
        response.setUser(userInfo);
        
        response.setContent(post.getContent());
        response.setImages(post.getImageList());  // Sử dụng helper method
        
        // Safely get counts (avoid lazy loading exception)
        try {
            response.setLikesCount(post.getLikes() != null ? post.getLikes().size() : 0);
        } catch (Exception e) {
            response.setLikesCount(0);
        }
        
        try {
            response.setCommentsCount(post.getComments() != null ? post.getComments().size() : 0);
        } catch (Exception e) {
            response.setCommentsCount(0);
        }
        
        response.setCreatedAt(post.getCreatedAt());
        
        // Check if current user liked this post
        try {
            if (currentUserId != null && post.getLikes() != null) {
                response.setLikedByCurrentUser(
                    post.getLikes().stream()
                        .anyMatch(like -> like.getUser().getId().equals(currentUserId))
                );
            } else {
                response.setLikedByCurrentUser(false);
            }
        } catch (Exception e) {
            response.setLikedByCurrentUser(false);
        }
        
        return response;
    }
}
