package com.example.project_backend04.service.IService;

import com.example.project_backend04.dto.request.Story.CreateStoryRequest;
import com.example.project_backend04.dto.response.Story.StoryResponseDto;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.enums.StoryStatus;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IStoryService {
    StoryResponseDto updateStoryStatus(Long storyId, StoryStatus newStatus, User admin);

        /**
         * Tạo story mới (status = PENDING)
         */
    StoryResponseDto createStory(CreateStoryRequest request, User user);
    
    /**
     * Lấy tất cả stories đã được approve (public)
     */
    List<StoryResponseDto> getActiveStories();
    
    /**
     * Lấy tất cả stories đã được approve với pagination (public)
     */
    Page<StoryResponseDto> getActiveStories(int page, int size);
    
    /**
     * Lấy stories của user cụ thể (đã approve)
     */
    List<StoryResponseDto> getStoriesByUser(Long userId);
    
    /**
     * Lấy stories của user cụ thể với pagination (đã approve)
     */
    Page<StoryResponseDto> getStoriesByUser(Long userId, int page, int size);
    
    /**
     * Lấy stories theo tag (đã approve)
     */
    List<StoryResponseDto> getStoriesByTag(String tag);
    
    /**
     * Lấy story theo ID
     */
    StoryResponseDto getStoryById(Long storyId);
    
    /**
     * Xóa story (chỉ owner hoặc admin mới xóa được)
     */
    void deleteStory(Long storyId, User user);
    
    /**
     * Xóa tất cả stories đã hết hạn (scheduled task)
     */
    void deleteExpiredStories();
    
    /**
     * Đếm số stories của user (đã approve)
     */
    long countUserStories(Long userId);
    
    // ==================== ADMIN METHODS ====================
    
    /**
     * Lấy tất cả stories đang chờ duyệt (PENDING) - Admin only
     */
    List<StoryResponseDto> getPendingStories();
    
    /**
     * Lấy tất cả stories đang chờ duyệt với pagination (PENDING) - Admin only
     */
    Page<StoryResponseDto> getPendingStories(int page, int size);
    
    /**
     * Approve story - Admin only
     */
    StoryResponseDto approveStory(Long storyId, User admin);
    
    /**
     * Reject story - Admin only
     */
    void rejectStory(Long storyId, User admin);
    
    /**
     * Đếm số stories đang chờ duyệt
     */
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
    
    /**
     * Cập nhật trạng thái story - Admin only
     * Lưu ý: Stories đã reject không thể update được
     */
}

