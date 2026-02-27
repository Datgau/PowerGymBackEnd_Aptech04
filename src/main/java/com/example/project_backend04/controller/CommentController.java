package com.example.project_backend04.controller;

import com.example.project_backend04.dto.request.Post.CreateCommentRequest;
import com.example.project_backend04.dto.response.Comment.CommentDTO;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.service.IService.ICommentService;
import com.example.project_backend04.service.IService.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final ICommentService commentService;
    private final IUserService userService;

    @PostMapping("/api/posts/{postId}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable Long postId,
            @RequestBody CreateCommentRequest request,
            Authentication authentication
    ) {
        System.out.println("=== COMMENT REQUEST RECEIVED ===");
        System.out.println("Post ID: " + postId);
        System.out.println("Content: " + (request != null ? request.getContent() : "null"));
        System.out.println("User: " + (authentication != null ? authentication.getName() : "null"));
        
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            var comment = commentService.addComment(postId, user.getId(), request.getContent());
            CommentDTO dto = CommentDTO.fromEntity(comment);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("comment", dto);
            result.put("message", "Đã thêm bình luận");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/api/posts/{postId}/comments")
    public ResponseEntity<?> getComments(@PathVariable Long postId) {
        try {
            var comments = commentService.getCommentsByPostId(postId);
            var dtos = comments.stream()
                    .map(CommentDTO::fromEntity)
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("comments", dtos);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/api/posts/comments/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long commentId,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            commentService.deleteComment(commentId, user.getId());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Đã xóa bình luận");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
