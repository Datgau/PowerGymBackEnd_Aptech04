package com.example.project_backend04.controller;

import com.example.project_backend04.dto.request.Post.CreatePostRequest;
import com.example.project_backend04.dto.response.Post.PostResponse;
import com.example.project_backend04.entity.Post;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.service.IService.IPostService;
import com.example.project_backend04.service.IService.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final IPostService postService;
    private final IUserService userService;

    /**
     * Tạo post mới với JSON (content + imageUrls)
     * POST /api/posts
     */
    @PostMapping
    public ResponseEntity<?> createPost(
            @RequestBody CreatePostRequest request,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Post post = postService.createPostWithUrls(user, request.getContent(), request.getImageUrls());
            PostResponse response = PostResponse.fromEntity(post, user.getId());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Post created successfully");
            result.put("data", response);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to create post: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Lấy feed posts
     * GET /api/posts/feed
     */
    @GetMapping("/feed")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Post> posts = postService.getFeedPosts(page, size);
            List<PostResponse> responses = posts.stream()
                    .map(post -> PostResponse.fromEntity(post, currentUser.getId()))
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", responses);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to get feed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Lấy chi tiết một post
     * GET /api/posts/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPost(
            @PathVariable Long id,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Post post = postService.getPostById(id);
            PostResponse response = PostResponse.fromEntity(post, currentUser.getId());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Post not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * Cập nhật nội dung post
     * PUT /api/posts/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePost(
            @PathVariable Long id,
            @RequestParam("content") String content,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Post post = postService.getPostById(id);
            
            // Check ownership
            if (!post.getUser().getId().equals(currentUser.getId())) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "You don't have permission to edit this post");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            Post updatedPost = postService.updatePostContent(id, content);
            PostResponse response = PostResponse.fromEntity(updatedPost, currentUser.getId());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Post updated successfully");
            result.put("data", response);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to update post: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Thêm ảnh vào post
     * POST /api/posts/{id}/images
     */
    @PostMapping("/{id}/images")
    public ResponseEntity<?> addImages(
            @PathVariable Long id,
            @RequestParam("images") List<MultipartFile> images,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Post post = postService.getPostById(id);
            
            // Check ownership
            if (!post.getUser().getId().equals(currentUser.getId())) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "You don't have permission to edit this post");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            Post updatedPost = postService.addImagesToPost(id, images);
            PostResponse response = PostResponse.fromEntity(updatedPost, currentUser.getId());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Images added successfully");
            result.put("data", response);

            return ResponseEntity.ok(result);
        } catch (IOException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to upload images: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to add images: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Xóa một ảnh khỏi post
     * DELETE /api/posts/{id}/images
     */
    @DeleteMapping("/{id}/images")
    public ResponseEntity<?> removeImage(
            @PathVariable Long id,
            @RequestParam("imageUrl") String imageUrl,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Post post = postService.getPostById(id);
            
            // Check ownership
            if (!post.getUser().getId().equals(currentUser.getId())) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "You don't have permission to edit this post");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            Post updatedPost = postService.removeImageFromPost(id, imageUrl);
            PostResponse response = PostResponse.fromEntity(updatedPost, currentUser.getId());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Image removed successfully");
            result.put("data", response);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to remove image: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Xóa post
     * DELETE /api/posts/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePost(
            @PathVariable Long id,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Post post = postService.getPostById(id);
            
            // Check ownership
            if (!post.getUser().getId().equals(currentUser.getId())) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "You don't have permission to delete this post");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            postService.deletePost(id);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Post deleted successfully");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to delete post: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Lấy posts của một user
     * GET /api/posts/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserPosts(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            User targetUser = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Post> posts = postService.getPostsByUser(targetUser);
            List<PostResponse> responses = posts.stream()
                    .map(post -> PostResponse.fromEntity(post, currentUser.getId()))
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", responses);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to get user posts: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
