package com.example.project_backend04.service;

import com.example.project_backend04.entity.Story;
import com.example.project_backend04.entity.StoryLike;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.repository.StoryLikeRepository;
import com.example.project_backend04.repository.StoryRepository;
import com.example.project_backend04.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import com.example.project_backend04.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StoryLikeService {

    private final StoryLikeRepository likeRepository;
    private final StoryRepository storyRepository;
    private final UserRepository userRepository;

    @Transactional
    public void likeStory(Long storyId) {
        User currentUser = getCurrentUser();
        
        Story story = storyRepository.findByIdWithUser(storyId);
        if (story == null) {
            throw new RuntimeException("Story not found");
        }

        // Check if story is approved and active
        if (!story.getIsActive() || story.getStatus() != com.example.project_backend04.enums.StoryStatus.APPROVED) {
            throw new RuntimeException("Cannot like this story");
        }

        // Check if user already liked this story
        if (likeRepository.existsByStoryAndUser(story, currentUser)) {
            throw new RuntimeException("You have already liked this story");
        }

        StoryLike like = new StoryLike();
        like.setStory(story);
        like.setUser(currentUser);

        likeRepository.save(like);

        // Update story like count
        story.incrementLikeCount();
        storyRepository.save(story);
    }

    @Transactional
    public void unlikeStory(Long storyId) {
        User currentUser = getCurrentUser();
        
        Story story = storyRepository.findByIdWithUser(storyId);
        if (story == null) {
            throw new RuntimeException("Story not found");
        }

        Optional<StoryLike> existingLike = likeRepository.findByStoryAndUser(story, currentUser);
        
        if (existingLike.isEmpty()) {
            throw new RuntimeException("You have not liked this story");
        }

        likeRepository.delete(existingLike.get());

        // Update story like count
        story.decrementLikeCount();
        storyRepository.save(story);
    }

    @Transactional(readOnly = true)
    public boolean isStoryLikedByUser(Long storyId, Long userId) {
        Story story = storyRepository.findByIdWithUser(storyId);
        if (story == null) {
            throw new RuntimeException("Story not found");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return likeRepository.existsByStoryAndUser(story, user);
    }

    @Transactional(readOnly = true)
    public Long getLikeCount(Long storyId) {
        Story story = storyRepository.findByIdWithUser(storyId);
        if (story == null) {
            throw new RuntimeException("Story not found");
        }
        
        return likeRepository.countByStory(story);
    }

    private User getCurrentUser() {
        return SecurityUtils.getCurrentUser();
    }
}