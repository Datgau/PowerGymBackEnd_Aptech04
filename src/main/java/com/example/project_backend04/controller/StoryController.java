package com.example.project_backend04.controller;

import com.example.project_backend04.dto.request.Story.CreateStoryRequest;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.dto.response.Story.StoryResponseDto;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.repository.UserRepository;
import com.example.project_backend04.security.CustomUserDetails;
import com.example.project_backend04.service.IService.IStoryService;
import lombok.RequiredArgsConstructor;
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
            @PathVariable Long storyId
    ) {
        try {
            StoryResponseDto story = storyService.getStoryById(storyId);
            return ResponseEntity.ok(ApiResponse.success(story));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get story: " + e.getMessage()));
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
}
