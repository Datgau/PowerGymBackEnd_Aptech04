package com.example.project_backend04.dto.response.Comment;

import com.example.project_backend04.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {
    private Long id;
    private Long postId;
    private String content;
    private LocalDateTime createdAt;
    
    // User info
    private Long userId;
    private String username;
    private String fullName;
    private String avatar;

    public static CommentDTO fromEntity(Comment comment) {
        return CommentDTO.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .userId(comment.getUser().getId())
                .username(comment.getUser().getUsername())
                .fullName(comment.getUser().getFullName())
                .avatar(comment.getUser().getAvatar())
                .build();
    }
}
