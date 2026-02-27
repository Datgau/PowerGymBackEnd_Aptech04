package com.example.project_backend04.controller;

import com.example.project_backend04.entity.User;
import com.example.project_backend04.service.IService.ILikeService;
import com.example.project_backend04.service.IService.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class LikeController {

    private final ILikeService likeService;
    private final IUserService userService;

    @PostMapping("/api/posts/{postId}/like")
    public ResponseEntity<?> toggleLike(
            @PathVariable Long postId,
            Authentication authentication
    ) {
        System.out.println("=== LIKE REQUEST RECEIVED ===");
        System.out.println("Post ID: " + postId);
        System.out.println("User: " + (authentication != null ? authentication.getName() : "null"));
        
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            boolean isLiked = likeService.toggleLike(postId, user.getId());
            int likeCount = likeService.getLikeCount(postId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("isLiked", isLiked);
            result.put("likeCount", likeCount);
            result.put("message", isLiked ? "Đã thích bài viết" : "Đã bỏ thích");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/api/posts/{postId}/likes")
    public ResponseEntity<?> getLikes(@PathVariable Long postId) {
        try {
            int likeCount = likeService.getLikeCount(postId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("likeCount", likeCount);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
