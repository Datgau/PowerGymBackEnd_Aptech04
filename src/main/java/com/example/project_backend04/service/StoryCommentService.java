package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.Story.CreateCommentRequest;
import com.example.project_backend04.dto.response.Story.StoryCommentResponse;
import com.example.project_backend04.dto.response.User.UserResponse;
import com.example.project_backend04.entity.Story;
import com.example.project_backend04.entity.StoryComment;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.mapper.UserMapper;
import com.example.project_backend04.repository.StoryCommentRepository;
import com.example.project_backend04.repository.StoryRepository;
import com.example.project_backend04.repository.UserRepository;
import com.example.project_backend04.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoryCommentService {

    private final StoryCommentRepository commentRepository;
    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public Page<StoryCommentResponse> getCommentsByStory(Long storyId, int page, int size) {
        Story story = storyRepository.findByIdWithUser(storyId);
        if (story == null) {
            throw new RuntimeException("Story not found");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<StoryComment> commentPage = commentRepository.findByStoryAndIsActiveTrueOrderByCreatedAtDesc(story, pageable);
        
        return commentPage.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<StoryCommentResponse> getCommentsByStoryLegacy(Long storyId) {
        Story story = storyRepository.findByIdWithUser(storyId);
        if (story == null) {
            throw new RuntimeException("Story not found");
        }

        List<StoryComment> comments = commentRepository.findByStoryAndIsActiveTrueOrderByCreatedAtDesc(story);
        return comments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public StoryCommentResponse addComment(Long storyId, CreateCommentRequest request) {
        User currentUser = getCurrentUser();
        
        Story story = storyRepository.findByIdWithUser(storyId);
        if (story == null) {
            throw new RuntimeException("Story not found");
        }

        // Check if story is approved and active
        if (!story.getIsActive() || story.getStatus() != com.example.project_backend04.enums.StoryStatus.APPROVED) {
            throw new RuntimeException("Cannot comment on this story");
        }

        StoryComment comment = new StoryComment();
        comment.setStory(story);
        comment.setUser(currentUser);
        comment.setContent(request.getContent());

        StoryComment savedComment = commentRepository.save(comment);

        // Update story comment count
        story.incrementCommentCount();
        storyRepository.save(story);

        return mapToResponse(savedComment);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        User currentUser = getCurrentUser();
        
        StoryComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        // Check if user owns the comment or is admin/staff
        if (!comment.getUser().getId().equals(currentUser.getId()) && 
            !currentUser.getRole().getName().equals("ADMIN") &&
            !currentUser.getRole().getName().equals("STAFF")) {
            throw new RuntimeException("You can only delete your own comments");
        }

        comment.setIsActive(false);
        commentRepository.save(comment);
        Story story = comment.getStory();
        story.decrementCommentCount();
        storyRepository.save(story);
    }

    @Transactional(readOnly = true)
    public Long getCommentCount(Long storyId) {
        Story story = storyRepository.findByIdWithUser(storyId);
        if (story == null) {
            throw new RuntimeException("Story not found");
        }
        
        return commentRepository.countByStoryAndIsActiveTrue(story);
    }

    private User getCurrentUser() {
        return SecurityUtils.getCurrentUser();
    }

    private StoryCommentResponse mapToResponse(StoryComment comment) {
        UserResponse userResponse = userMapper.toResponse(comment.getUser());
        
        return StoryCommentResponse.builder()
                .id(comment.getId())
                .user(userResponse)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .timeAgo(calculateTimeAgo(comment.getCreatedAt()))
                .build();
    }

    private String calculateTimeAgo(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(dateTime, now);
        long hours = ChronoUnit.HOURS.between(dateTime, now);
        long days = ChronoUnit.DAYS.between(dateTime, now);

        if (minutes < 1) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + "m ago";
        } else if (hours < 24) {
            return hours + "h ago";
        } else if (days < 7) {
            return days + "d ago";
        } else if (days < 30) {
            long weeks = days / 7;
            return weeks + "w ago";
        } else {
            long months = days / 30;
            return months + "mo ago";
        }
    }
}