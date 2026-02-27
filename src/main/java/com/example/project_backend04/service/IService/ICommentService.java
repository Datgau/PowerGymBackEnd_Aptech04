package com.example.project_backend04.service.IService;

import com.example.project_backend04.entity.Comment;
import java.util.List;

public interface ICommentService {
    Comment addComment(Long postId, Long userId, String content);
    List<Comment> getCommentsByPostId(Long postId);
    void deleteComment(Long commentId, Long userId);
}
