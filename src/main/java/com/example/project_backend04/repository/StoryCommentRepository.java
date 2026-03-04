package com.example.project_backend04.repository;

import com.example.project_backend04.entity.Story;
import com.example.project_backend04.entity.StoryComment;
import com.example.project_backend04.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoryCommentRepository extends JpaRepository<StoryComment, Long> {

    /**
     * Find comments by story with pagination, ordered by creation date
     */
    @Query("SELECT sc FROM StoryComment sc JOIN FETCH sc.user WHERE sc.story = :story AND sc.isActive = true ORDER BY sc.createdAt DESC")
    Page<StoryComment> findByStoryAndIsActiveTrueOrderByCreatedAtDesc(@Param("story") Story story, Pageable pageable);

    /**
     * Find comments by story without pagination
     */
    @Query("SELECT sc FROM StoryComment sc JOIN FETCH sc.user WHERE sc.story = :story AND sc.isActive = true ORDER BY sc.createdAt DESC")
    List<StoryComment> findByStoryAndIsActiveTrueOrderByCreatedAtDesc(@Param("story") Story story);

    /**
     * Count active comments for a story
     */
    Long countByStoryAndIsActiveTrue(Story story);

    /**
     * Find comments by user
     */
    List<StoryComment> findByUserAndIsActiveTrueOrderByCreatedAtDesc(User user);

    /**
     * Check if user has commented on a story
     */
    boolean existsByStoryAndUserAndIsActiveTrue(Story story, User user);
}