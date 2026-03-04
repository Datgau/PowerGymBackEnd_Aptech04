package com.example.project_backend04.controller;

import com.example.project_backend04.dto.request.Story.CreateCommentRequest;
import com.example.project_backend04.dto.request.Story.CreateStoryRequest;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.dto.response.Story.StoryCommentResponse;
import com.example.project_backend04.dto.response.Story.StoryResponseDto;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.repository.UserRepository;
import com.example.project_backend04.security.CustomUserDetails;
import com.example.project_backend04.service.IService.IStoryService;
import com.example.project_backend04.service.StoryCommentService;
import com.example.project_backend04.service.StoryLikeService;
import com.example.project_backend04.service.StoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
public class StoryController {

    private final IStoryService storyService;
    private final StoryService storyServiceImpl;
    private final StoryLikeService storyLikeService;
    private final StoryCommentService storyCommentService;
    private final UserRepository userRepository;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<StoryResponseDto>> createStory(
            @ModelAttribute CreateStoryRequest request,
            Authentication authentication
    ) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            StoryResponseDto story = storyService.createStory(request, user);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(story, "Story created successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create story: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StoryResponseDto>>> getAllActiveStories() {
        try {
            List<StoryResponseDto> stories = storyService.getActiveStories();
            return ResponseEntity.ok(ApiResponse.success(stories));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get stories: " + e.getMessage()));
        }
    }

    @GetMapping("/paginated")
    public ResponseEntity<ApiResponse<Page<StoryResponseDto>>> getAllActiveStoriesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Page<StoryResponseDto> stories = storyService.getActiveStories(page, size);
            return ResponseEntity.ok(ApiResponse.success(stories));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get stories: " + e.getMessage()));
        }
    }


    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<StoryResponseDto>>> getStoriesByUser(
            @PathVariable Long userId
    ) {
        try {
            List<StoryResponseDto> stories = storyService.getStoriesByUser(userId);
            return ResponseEntity.ok(ApiResponse.success(stories));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get user stories: " + e.getMessage()));
        }
    }

    @GetMapping("/my-stories")
    public ResponseEntity<ApiResponse<List<StoryResponseDto>>> getMyStories(
            Authentication authentication
    ) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            List<StoryResponseDto> stories = storyService.getStoriesByUser(userDetails.getId());
            return ResponseEntity.ok(ApiResponse.success(stories));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get stories: " + e.getMessage()));
        }
    }

    @GetMapping("/my-stories/paginated")
    public ResponseEntity<ApiResponse<Page<StoryResponseDto>>> getMyStoriesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Page<StoryResponseDto> stories = storyService.getStoriesByUser(userDetails.getId(), page, size);
            return ResponseEntity.ok(ApiResponse.success(stories));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get stories: " + e.getMessage()));
        }
    }

    @GetMapping("/tag/{tag}")
    public ResponseEntity<ApiResponse<List<StoryResponseDto>>> getStoriesByTag(
            @PathVariable String tag
    ) {
        try {
            List<StoryResponseDto> stories = storyService.getStoriesByTag(tag);
            return ResponseEntity.ok(ApiResponse.success(stories));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get stories by tag: " + e.getMessage()));
        }
    }


    @GetMapping("/{storyId}")
    public ResponseEntity<ApiResponse<StoryResponseDto>> getStoryById(
            @PathVariable Long storyId,
            Authentication authentication
    ) {
        try {
            StoryResponseDto story;

            if (authentication != null && authentication.isAuthenticated()) {
                // User is logged in, include user-specific information
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                story = storyServiceImpl.getStoryByIdWithUserInfo(storyId, userDetails.getId());
            } else {
                // Anonymous user
                story = storyService.getStoryById(storyId);
            }

            return ResponseEntity.ok(ApiResponse.success(story));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get story: " + e.getMessage()));
        }
    }

    // ==================== LIKE ENDPOINTS ====================

    @PostMapping("/{storyId}/like")
    public ResponseEntity<ApiResponse<Void>> likeStory(
            @PathVariable Long storyId,
            Authentication authentication
    ) {
        try {
            storyLikeService.likeStory(storyId);
            return ResponseEntity.ok(ApiResponse.success(null, "Story liked successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to like story: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{storyId}/like")
    public ResponseEntity<ApiResponse<Void>> unlikeStory(
            @PathVariable Long storyId,
            Authentication authentication
    ) {
        try {
            storyLikeService.unlikeStory(storyId);
            return ResponseEntity.ok(ApiResponse.success(null, "Story unliked successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to unlike story: " + e.getMessage()));
        }
    }

    // ==================== COMMENT ENDPOINTS ====================

    @GetMapping("/{storyId}/comments")
    public ResponseEntity<ApiResponse<List<StoryCommentResponse>>> getStoryComments(
            @PathVariable Long storyId
    ) {
        try {
            List<StoryCommentResponse> comments = storyCommentService.getCommentsByStoryLegacy(storyId);
            return ResponseEntity.ok(ApiResponse.success(comments));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get comments: " + e.getMessage()));
        }
    }

    @GetMapping("/{storyId}/comments/paginated")
    public ResponseEntity<ApiResponse<Page<StoryCommentResponse>>> getStoryCommentsPaginated(
            @PathVariable Long storyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Page<StoryCommentResponse> comments = storyCommentService.getCommentsByStory(storyId, page, size);
            return ResponseEntity.ok(ApiResponse.success(comments));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get comments: " + e.getMessage()));
        }
    }

    @PostMapping("/{storyId}/comments")
    public ResponseEntity<ApiResponse<StoryCommentResponse>> addComment(
            @PathVariable Long storyId,
            @RequestBody CreateCommentRequest request,
            Authentication authentication
    ) {
        try {
            StoryCommentResponse comment = storyCommentService.addComment(storyId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(comment, "Comment added successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to add comment: " + e.getMessage()));
        }
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            Authentication authentication
    ) {
        try {
            storyCommentService.deleteComment(commentId);
            return ResponseEntity.ok(ApiResponse.success(null, "Comment deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete comment: " + e.getMessage()));
        }
    }


    @DeleteMapping("/{storyId}")
    public ResponseEntity<ApiResponse<Void>> deleteStory(
            @PathVariable Long storyId,
            Authentication authentication
    ) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            storyService.deleteStory(storyId, user);

            return ResponseEntity.ok(ApiResponse.success(null, "Story deleted successfully"));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("permission")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete story: " + e.getMessage()));
        }
    }


    @GetMapping("/count/user/{userId}")
    public ResponseEntity<ApiResponse<Long>> countUserStories(
            @PathVariable Long userId
    ) {
        try {
            long count = storyService.countUserStories(userId);
            return ResponseEntity.ok(ApiResponse.success(count));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to count stories: " + e.getMessage()));
        }
    }

    // ==================== ADMIN ENDPOINTS ====================

    @GetMapping("/admin/pending")
    public ResponseEntity<ApiResponse<List<StoryResponseDto>>> getPendingStories(
            Authentication authentication
    ) {
        try {
            List<StoryResponseDto> stories = storyService.getPendingStories();
            return ResponseEntity.ok(ApiResponse.success(stories));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get pending stories: " + e.getMessage()));
        }
    }

    @GetMapping("/admin/pending/paginated")
    public ResponseEntity<ApiResponse<Page<StoryResponseDto>>> getPendingStoriesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        try {
            Page<StoryResponseDto> stories = storyService.getPendingStories(page, size);
            return ResponseEntity.ok(ApiResponse.success(stories));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get pending stories: " + e.getMessage()));
        }
    }

    @PutMapping("/admin/{storyId}/approve")
    public ResponseEntity<ApiResponse<StoryResponseDto>> approveStory(
            @PathVariable Long storyId,
            Authentication authentication
    ) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User admin = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            StoryResponseDto story = storyService.approveStory(storyId, admin);

            return ResponseEntity.ok(ApiResponse.success(story, "Story approved successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to approve story: " + e.getMessage()));
        }
    }

    @PutMapping("/admin/{storyId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectStory(
            @PathVariable Long storyId,
            Authentication authentication
    ) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User admin = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            storyService.rejectStory(storyId, admin);

            return ResponseEntity.ok(ApiResponse.success(null, "Story rejected successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to reject story: " + e.getMessage()));
        }
    }

    @GetMapping("/admin/pending/count")
    public ResponseEntity<ApiResponse<Long>> countPendingStories() {
        try {
            long count = storyService.countPendingStories();
            return ResponseEntity.ok(ApiResponse.success(count));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to count pending stories: " + e.getMessage()));
        }
    }

    @GetMapping("/admin/approved/count")
    public ResponseEntity<ApiResponse<Long>> countApprovedStories() {
        try {
            long count = storyService.countApprovedStories();
            return ResponseEntity.ok(ApiResponse.success(count));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to count approved stories: " + e.getMessage()));
        }
    }

    @GetMapping("/admin/rejected/count")
    public ResponseEntity<ApiResponse<Long>> countRejectedStories() {
        try {
            long count = storyService.countRejectedStories();
            return ResponseEntity.ok(ApiResponse.success(count));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to count rejected stories: " + e.getMessage()));
        }
    }

    @GetMapping("/admin/all")
    public ResponseEntity<ApiResponse<List<StoryResponseDto>>> getAllStoriesForAdmin(
            Authentication authentication
    ) {
        try {
            List<StoryResponseDto> stories = storyService.getAllStoriesForAdmin();
            return ResponseEntity.ok(ApiResponse.success(stories));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get all stories: " + e.getMessage()));
        }
    }

    @GetMapping("/admin/all/paginated")
    public ResponseEntity<ApiResponse<Page<StoryResponseDto>>> getAllStoriesForAdminPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        try {
            Page<StoryResponseDto> stories = storyService.getAllStoriesForAdmin(page, size);
            return ResponseEntity.ok(ApiResponse.success(stories));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get all stories: " + e.getMessage()));
        }
    }

    @GetMapping("/admin/status/{status}")
    public ResponseEntity<ApiResponse<List<StoryResponseDto>>> getStoriesByStatus(
            @PathVariable String status,
            Authentication authentication
    ) {
        try {
            com.example.project_backend04.enums.StoryStatus storyStatus =
                    com.example.project_backend04.enums.StoryStatus.valueOf(status.toUpperCase());
            List<StoryResponseDto> stories = storyService.getStoriesByStatus(storyStatus);
            return ResponseEntity.ok(ApiResponse.success(stories));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid status: " + status));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get stories by status: " + e.getMessage()));
        }
    }

    @PutMapping("/admin/{storyId}/status")
    public ResponseEntity<ApiResponse<StoryResponseDto>> updateStoryStatus(
            @PathVariable Long storyId,
            @RequestParam String status,
            Authentication authentication
    ) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User admin = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            com.example.project_backend04.enums.StoryStatus newStatus =
                    com.example.project_backend04.enums.StoryStatus.valueOf(status.toUpperCase());

            StoryResponseDto story = storyService.updateStoryStatus(storyId, newStatus, admin);

            return ResponseEntity.ok(ApiResponse.success(story, "Story status updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid status: " + status));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update story status: " + e.getMessage()));
        }
    }
}
