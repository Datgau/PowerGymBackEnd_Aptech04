package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.Story.CreateStoryRequest;
import com.example.project_backend04.dto.response.Story.StoryResponseDto;
import com.example.project_backend04.entity.Story;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.enums.StoryStatus;
import com.example.project_backend04.mapper.StoriesMapper;
import com.example.project_backend04.repository.StoryRepository;
import com.example.project_backend04.repository.UserRepository;
import com.example.project_backend04.service.IService.IStoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoryService implements IStoryService {

    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final StoriesMapper storiesMapper;

    @Override
    @Transactional
    public StoryResponseDto createStory(CreateStoryRequest request, User user) {
        try {
            if (request.getImage() == null || request.getImage().isEmpty()) {
                throw new IllegalArgumentException("Image is required");
            }
            String imageUrl = cloudinaryService.uploadStory(request.getImage());
            Story story = new Story();
            story.setUser(user);
            story.setImageUrl(imageUrl);
            story.setTitle(request.getTitle());
            story.setTag(request.getTag());
            story.setContent(request.getContent());
            story.setIsActive(true);
            story.setStatus(StoryStatus.PENDING);
            story.setCreatedAt(LocalDateTime.now());
            story.setExpiresAt(LocalDateTime.now().plusHours(24));
            Story savedStory = storyRepository.save(story);

            return storiesMapper.toDto(savedStory);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create story: " + e.getMessage(), e);
        }
    }

    @Override
    public List<StoryResponseDto> getActiveStories() {
        // Only return APPROVED stories
        List<Story> stories = storyRepository.findActiveStoriesWithUser(LocalDateTime.now());
        return stories.stream()
                .map(storiesMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<StoryResponseDto> getStoriesByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Story> stories = storyRepository.findActiveStoriesByUser(user, LocalDateTime.now());
        return stories.stream()
                .map(storiesMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<StoryResponseDto> getStoriesByTag(String tag) {
        List<Story> stories = storyRepository.findActiveStoriesByTag(tag, LocalDateTime.now());
        return stories.stream()
                .map(storiesMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public StoryResponseDto getStoryById(Long storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found"));
        if (story.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Story has expired");
        }
        
        return storiesMapper.toDto(story);
    }

    @Override
    @Transactional
    public void deleteStory(Long storyId, User user) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found"));
        boolean isOwner = story.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() != null &&
                user.getRole().getName().equals("ROLE_ADMIN");

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("You don't have permission to delete this story");
        }
        try {
            cloudinaryService.deleteFile(story.getImageUrl());
        } catch (Exception e) {
            System.err.println("[Warning] Failed to delete story image: " + e.getMessage());
        }

        storyRepository.delete(story);
    }

    @Override
    @Transactional
    public void deleteExpiredStories() {
        List<Story> expiredStories = storyRepository.findByExpiresAtBefore(LocalDateTime.now());
        
        for (Story story : expiredStories) {
            try {
                cloudinaryService.deleteFile(story.getImageUrl());
            } catch (Exception e) {
                System.err.println("[Warning] Failed to delete expired story image: " + e.getMessage());
            }
        }

        storyRepository.deleteAll(expiredStories);
        System.out.println("[Info] Deleted " + expiredStories.size() + " expired stories");
    }

    @Override
    public long countUserStories(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return storyRepository.countActiveStoriesByUser(user, LocalDateTime.now());
    }

    // ==================== ADMIN METHODS ====================

    @Override
    public List<StoryResponseDto> getPendingStories() {
        List<Story> stories = storyRepository.findPendingStories();
        return stories.stream()
                .map(storiesMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StoryResponseDto approveStory(Long storyId, User admin) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found"));

        if (story.getStatus() != StoryStatus.PENDING) {
            throw new RuntimeException("Story is not in pending status");
        }

        story.setStatus(StoryStatus.APPROVED);
        story.setApprovedBy(admin);
        story.setApprovedAt(LocalDateTime.now());

        Story savedStory = storyRepository.save(story);
        return storiesMapper.toDto(savedStory);
    }

    @Override
    @Transactional
    public void rejectStory(Long storyId, User admin) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found"));

        if (story.getStatus() != StoryStatus.PENDING) {
            throw new RuntimeException("Story is not in pending status");
        }

        story.setStatus(StoryStatus.REJECTED);
        story.setApprovedBy(admin);
        story.setApprovedAt(LocalDateTime.now());

        storyRepository.save(story);
        try {
            cloudinaryService.deleteFile(story.getImageUrl());
        } catch (Exception e) {
            System.err.println("[Warning] Failed to delete rejected story image: " + e.getMessage());
        }
    }

    @Override
    public long countPendingStories() {
        return storyRepository.countPendingStories();
    }


}

