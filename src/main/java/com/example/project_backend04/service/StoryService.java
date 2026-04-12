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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final StoryLikeService storyLikeService;
    private final StoryCommentService storyCommentService;

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
            story.setExpiresAt(LocalDateTime.now().plusHours(168));
            Story savedStory = storyRepository.save(story);

            return storiesMapper.toDto(savedStory);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create story: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoryResponseDto> getActiveStories() {
        List<Story> stories = storyRepository.findActiveStoriesWithUser(LocalDateTime.now());
        return stories.stream()
                .map(storiesMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StoryResponseDto> getActiveStories(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Story> storyPage = storyRepository.findActiveStoriesWithUserPaginated(LocalDateTime.now(), pageable);
        return storyPage.map(storiesMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoryResponseDto> getStoriesByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Story> stories = storyRepository.findActiveStoriesByUser(user, LocalDateTime.now());
        return stories.stream()
                .map(storiesMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StoryResponseDto> getStoriesByUser(Long userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Story> storyPage = storyRepository.findActiveStoriesByUserPaginated(user, LocalDateTime.now(), pageable);
        return storyPage.map(storiesMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoryResponseDto> getStoriesByTag(String tag) {
        List<Story> stories = storyRepository.findActiveStoriesByTag(tag, LocalDateTime.now());
        return stories.stream()
                .map(storiesMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StoryResponseDto getStoryById(Long storyId) {
        Story story = storyRepository.findByIdWithUser(storyId);
        if (story == null) {
            throw new RuntimeException("Story not found");
        }

        // Check if story is approved and active
        if (!story.getIsActive() || story.getStatus() != StoryStatus.APPROVED) {
            throw new RuntimeException("Story not available");
        }

        if (story.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Story has expired");
        }

        return storiesMapper.toDto(story);
    }

    @Transactional(readOnly = true)
    public StoryResponseDto getStoryByIdWithUserInfo(Long storyId, Long currentUserId) {
        Story story = storyRepository.findByIdWithUser(storyId);
        if (story == null) {
            throw new RuntimeException("Story not found");
        }

        // Check if story is approved and active
        if (!story.getIsActive() || story.getStatus() != StoryStatus.APPROVED) {
            throw new RuntimeException("Story not available");
        }

        if (story.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Story has expired");
        }

        StoryResponseDto dto = storiesMapper.toDto(story);

        // Add user-specific information if user is logged in
        if (currentUserId != null) {
            boolean isLiked = storyLikeService.isStoryLikedByUser(storyId, currentUserId);
            dto.setIsLikedByCurrentUser(isLiked);
        }

        return dto;
    }

    @Override
    @Transactional
    public void deleteStory(Long storyId, User user) {
        Story story = storyRepository.findByIdWithUser(storyId);
        if (story == null) {
            throw new RuntimeException("Story not found");
        }

        boolean isOwner = story.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() != null &&
                (user.getRole().getName().equals("ROLE_ADMIN") || 
                 user.getRole().getName().equals("ROLE_STAFF"));

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
    public StoryResponseDto updateStory(Long storyId, CreateStoryRequest request, User user) {
        Story story = storyRepository.findByIdWithUser(storyId);
        if (story == null) {
            throw new RuntimeException("Story not found");
        }

        // Only owner can update
        if (!story.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to update this story");
        }

        // Update image if provided
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            try {
                // Delete old image
                cloudinaryService.deleteFile(story.getImageUrl());
                // Upload new image
                String imageUrl = cloudinaryService.uploadStory(request.getImage());
                story.setImageUrl(imageUrl);
            } catch (Exception e) {
                throw new RuntimeException("Failed to upload image: " + e.getMessage());
            }
        }
        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            story.setTitle(request.getTitle().trim());
        }
        if (request.getContent() != null) {
            story.setContent(request.getContent().trim());
        }
        if (request.getTag() != null) {
            story.setTag(request.getTag().trim());
        }

        story.setStatus(StoryStatus.PENDING);
        story.getCreatedAt().plusHours(168);

        Story updatedStory = storyRepository.save(story);
        return storiesMapper.toDto(updatedStory);
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
        System.out.println("[Info] Deleted " + expiredStories.size() + " expired stories (including rejected stories)");
    }

    @Override
    @Transactional(readOnly = true)
    public long countUserStories(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return storyRepository.countActiveStoriesByUser(user, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoryResponseDto> getPendingStories() {
        List<Story> stories = storyRepository.findPendingStories(LocalDateTime.now());
        return stories.stream()
                .map(storiesMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StoryResponseDto> getPendingStories(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Story> storyPage = storyRepository.findPendingStoriesPaginated(LocalDateTime.now(), pageable);
        return storyPage.map(storiesMapper::toDto);
    }

    @Override
    @Transactional
    public StoryResponseDto approveStory(Long storyId, User admin) {
        Story story = storyRepository.findByIdWithUser(storyId);
        if (story == null) {
            throw new RuntimeException("Story not found");
        }

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
        Story story = storyRepository.findByIdWithUser(storyId);
        if (story == null) {
            throw new RuntimeException("Story not found");
        }

        if (story.getStatus() != StoryStatus.PENDING) {
            throw new RuntimeException("Story is not in pending status");
        }
        story.setStatus(StoryStatus.REJECTED);
        story.setApprovedBy(admin);
        story.setApprovedAt(LocalDateTime.now());
        story.setExpiresAt(LocalDateTime.now());

        storyRepository.save(story);
        try {
            cloudinaryService.deleteFile(story.getImageUrl());
        } catch (Exception e) {
            System.err.println("[Warning] Failed to delete rejected story image: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingStories() {
        return storyRepository.countPendingStories(LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public long countApprovedStories() {
        return storyRepository.countApprovedStories(LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public long countRejectedStories() {
        return storyRepository.countRejectedStories(LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoryResponseDto> getAllStoriesForAdmin() {
        List<Story> stories = storyRepository.findAllStoriesWithUser(LocalDateTime.now());
        return stories.stream()
                .map(storiesMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StoryResponseDto> getAllStoriesForAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Story> storyPage = storyRepository.findAllStoriesWithUserPaginated(LocalDateTime.now(), pageable);
        return storyPage.map(storiesMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoryResponseDto> getStoriesByStatus(StoryStatus status) {
        List<Story> stories = storyRepository.findStoriesByStatus(status, LocalDateTime.now());
        return stories.stream()
                .map(storiesMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StoryResponseDto> getStoriesByStatus(StoryStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Story> storyPage = storyRepository.findStoriesByStatusPaginated(status, LocalDateTime.now(), pageable);
        return storyPage.map(storiesMapper::toDto);
    }

    @Override
    @Transactional
    public StoryResponseDto updateStoryStatus(Long storyId, StoryStatus newStatus, User admin) {
        Story story = storyRepository.findByIdWithUser(storyId);
        if (story == null) {
            throw new RuntimeException("Story not found");
        }
        if (story.getStatus() == StoryStatus.REJECTED) {
            throw new RuntimeException("Cannot update status of rejected stories");
        }
        if (newStatus == story.getStatus()) {
            throw new RuntimeException("Story is already in " + newStatus.name().toLowerCase() + " status");
        }
        story.setStatus(newStatus);
        story.setApprovedBy(admin);
        story.setApprovedAt(LocalDateTime.now());
        if (newStatus == StoryStatus.REJECTED) {
            story.setExpiresAt(LocalDateTime.now());
        }
        Story savedStory = storyRepository.save(story);
        return storiesMapper.toDto(savedStory);
    }
}

