package com.example.project_backend04.dto.response.Story;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryResponseDto {
    
    private Long id;
    
    private String imageUrl;
    
    private String title;
    
    private String tag;
    
    private String content;
    
    private Boolean isActive;
    
    private String status; // PENDING, APPROVED, REJECTED
    
    private LocalDateTime createdAt;
    
    private LocalDateTime expiresAt;
    
    private LocalDateTime approvedAt;
    
    // User info
    private Long userId;
    private String username;
    private String userAvatar;
    private String userFullName;
    
    // Additional info
    private Long viewCount;
    private Boolean isExpired;
    private String timeAgo; // "2 hours ago", "1 day ago"
}
