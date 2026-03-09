package com.example.project_backend04.repository;

import com.example.project_backend04.entity.Story;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.enums.StoryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {

    @Query("SELECT s FROM Story s JOIN FETCH s.user WHERE s.status = 'APPROVED' AND s.expiresAt > :now AND s.isActive = true ORDER BY s.createdAt DESC")
    List<Story> findActiveStoriesWithUser(@Param("now") LocalDateTime now);


    @Query("SELECT s FROM Story s JOIN FETCH s.user WHERE s.status = 'APPROVED' AND s.expiresAt > :now AND s.isActive = true ORDER BY s.createdAt DESC")
    Page<Story> findActiveStoriesWithUserPaginated(@Param("now") LocalDateTime now, Pageable pageable);


    @Query("SELECT s FROM Story s JOIN FETCH s.user WHERE s.user = :user AND s.status = 'APPROVED' AND s.expiresAt > :now AND s.isActive = true ORDER BY s.createdAt DESC")
    List<Story> findActiveStoriesByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    @Query("SELECT s FROM Story s JOIN FETCH s.user WHERE s.user = :user AND s.status = 'APPROVED' AND s.expiresAt > :now AND s.isActive = true ORDER BY s.createdAt DESC")
    Page<Story> findActiveStoriesByUserPaginated(@Param("user") User user, @Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT s FROM Story s JOIN FETCH s.user WHERE s.tag = :tag AND s.status = 'APPROVED' AND s.expiresAt > :now AND s.isActive = true ORDER BY s.createdAt DESC")
    List<Story> findActiveStoriesByTag(@Param("tag") String tag, @Param("now") LocalDateTime now);


    List<Story> findByExpiresAtBefore(LocalDateTime dateTime);


    @Query("SELECT COUNT(s) FROM Story s WHERE s.user = :user AND s.status = 'APPROVED' AND s.expiresAt > :now AND s.isActive = true")
    long countActiveStoriesByUser(@Param("user") User user, @Param("now") LocalDateTime now);


    @Query("SELECT s FROM Story s JOIN FETCH s.user WHERE s.status = 'PENDING' AND s.isActive = true AND s.expiresAt > :now ORDER BY s.createdAt DESC")
    List<Story> findPendingStories(@Param("now") LocalDateTime now);

    @Query("SELECT s FROM Story s JOIN FETCH s.user WHERE s.status = 'PENDING' AND s.isActive = true AND s.expiresAt > :now ORDER BY s.createdAt DESC")
    Page<Story> findPendingStoriesPaginated(@Param("now") LocalDateTime now, Pageable pageable);


    @Query("SELECT s FROM Story s JOIN FETCH s.user WHERE s.status = :status AND s.isActive = true AND s.expiresAt > :now ORDER BY s.createdAt DESC")
    List<Story> findStoriesByStatus(@Param("status") StoryStatus status, @Param("now") LocalDateTime now);

    @Query("SELECT s FROM Story s JOIN FETCH s.user WHERE s.status = :status AND s.isActive = true AND s.expiresAt > :now ORDER BY s.createdAt DESC")
    Page<Story> findStoriesByStatusPaginated(@Param("status") StoryStatus status, @Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT COUNT(s) FROM Story s WHERE s.status = 'PENDING' AND s.expiresAt > :now")
    long countPendingStories(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(s) FROM Story s WHERE s.status = 'APPROVED' AND s.expiresAt > :now")
    long countApprovedStories(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(s) FROM Story s WHERE s.status = 'REJECTED' AND s.expiresAt > :now")
    long countRejectedStories(@Param("now") LocalDateTime now);

    @Query("SELECT s FROM Story s JOIN FETCH s.user WHERE s.isActive = true AND s.expiresAt > :now ORDER BY s.createdAt DESC")
    List<Story> findAllStoriesWithUser(@Param("now") LocalDateTime now);

    @Query("SELECT s FROM Story s JOIN FETCH s.user WHERE s.isActive = true AND s.expiresAt > :now ORDER BY s.createdAt DESC")
    Page<Story> findAllStoriesWithUserPaginated(@Param("now") LocalDateTime now, Pageable pageable);

    /**
     * Find story by ID with user eagerly loaded
     */
    @Query("SELECT s FROM Story s JOIN FETCH s.user WHERE s.id = :id")
    Story findByIdWithUser(@Param("id") Long id);

    /**
     * Fix data inconsistencies - update stories that have approvedAt but wrong status
     */
    @Query("UPDATE Story s SET s.status = 'APPROVED' WHERE s.approvedAt IS NOT NULL AND s.status = 'PENDING'")
    int fixApprovedStoriesStatus();

    /**
     * Fix data inconsistencies - update stories that have approvedAt but wrong status for rejected
     */
    @Query("UPDATE Story s SET s.status = 'REJECTED' WHERE s.approvedAt IS NOT NULL AND s.status = 'PENDING' AND s.expiresAt <= :now")
    int fixRejectedStoriesStatus(@Param("now") LocalDateTime now);
}

