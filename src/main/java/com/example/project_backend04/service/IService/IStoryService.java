package com.example.project_backend04.service.IService;

import com.example.project_backend04.dto.request.Story.CreateStoryRequest;
import com.example.project_backend04.dto.response.Story.StoryResponseDto;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.enums.StoryStatus;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IStoryService {
    StoryResponseDto updateStoryStatus(Long storyId, StoryStatus newStatus, User admin);

    StoryResponseDto createStory(CreateStoryRequest request, User user);

    List<StoryResponseDto> getActiveStories();

    Page<StoryResponseDto> getActiveStories(int page, int size);
    

    List<StoryResponseDto> getStoriesByUser(Long userId);
    

    Page<StoryResponseDto> getStoriesByUser(Long userId, int page, int size);
    

    List<StoryResponseDto> getStoriesByTag(String tag);
    

    StoryResponseDto getStoryById(Long storyId);

    StoryResponseDto updateStory(Long storyId, CreateStoryRequest request, User user);
    

    void deleteStory(Long storyId, User user);

    void deleteExpiredStories();

    long countUserStories(Long userId);
    

    List<StoryResponseDto> getPendingStories();
    

    Page<StoryResponseDto> getPendingStories(int page, int size);

    StoryResponseDto approveStory(Long storyId, User admin);
    

    void rejectStory(Long storyId, User admin);

    long countPendingStories();
    
    /**
     * Đếm số stories đã được approve
     */
    long countApprovedStories();
    
    /**
     * Đếm số stories đã bị reject
     */
    long countRejectedStories();
    
    /**
     * Lấy tất cả stories cho admin (tất cả trạng thái)
     */
    List<StoryResponseDto> getAllStoriesForAdmin();
    
    /**
     * Lấy tất cả stories cho admin với pagination (tất cả trạng thái)
     */
    Page<StoryResponseDto> getAllStoriesForAdmin(int page, int size);
    
    /**
     * Lấy stories theo trạng thái cụ thể - Admin only
     */
    List<StoryResponseDto> getStoriesByStatus(com.example.project_backend04.enums.StoryStatus status);
    
    /**
     * Lấy stories theo trạng thái cụ thể với pagination - Admin only
     */
    Page<StoryResponseDto> getStoriesByStatus(com.example.project_backend04.enums.StoryStatus status, int page, int size);

}

