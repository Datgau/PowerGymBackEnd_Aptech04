package com.example.project_backend04.repository;

import com.example.project_backend04.entity.Story;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.enums.StoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {

    /**
     * Lấy tất cả stories đã được approve (hiển thị cho public)
     */
    @Query("SELECT s FROM Story s JOIN FETCH s.user WHERE s.status = 'APPROVED' AND s.expiresAt > :now AND s.isActive = true ORDER BY s.createdAt DESC")
    List<Story> findActiveStoriesWithUser(@Param("now") LocalDateTime now);

    /**
     * Lấy stories của user cụ thể (đã approve)
     */
    @Query("SELECT s FROM Story s WHERE s.user = :user AND s.status = 'APPROVED' AND s.expiresAt > :now AND s.isActive = true ORDER BY s.createdAt DESC")
    List<Story> findActiveStoriesByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    /**
     * Lấy stories theo tag (đã approve)
     */
    @Query("SELECT s FROM Story s WHERE s.tag = :tag AND s.status = 'APPROVED' AND s.expiresAt > :now AND s.isActive = true ORDER BY s.createdAt DESC")
    List<Story> findActiveStoriesByTag(@Param("tag") String tag, @Param("now") LocalDateTime now);

    /**
     * Lấy stories đã hết hạn (để xóa)
     */
    List<Story> findByExpiresAtBefore(LocalDateTime dateTime);

    /**
     * Đếm số stories còn hiệu lực của user (đã approve)
     */
    @Query("SELECT COUNT(s) FROM Story s WHERE s.user = :user AND s.status = 'APPROVED' AND s.expiresAt > :now AND s.isActive = true")
    long countActiveStoriesByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    /**
     * Lấy tất cả stories đang chờ duyệt (PENDING) - Admin only
     */
    @Query("SELECT s FROM Story s JOIN FETCH s.user WHERE s.status = 'PENDING' ORDER BY s.createdAt DESC")
    List<Story> findPendingStories();

    /**
     * Lấy stories theo status
     */
    @Query("SELECT s FROM Story s JOIN FETCH s.user WHERE s.status = :status ORDER BY s.createdAt DESC")
    List<Story> findStoriesByStatus(@Param("status") StoryStatus status);

    /**
     * Đếm số stories đang chờ duyệt
     */
    @Query("SELECT COUNT(s) FROM Story s WHERE s.status = 'PENDING'")
    long countPendingStories();
}

