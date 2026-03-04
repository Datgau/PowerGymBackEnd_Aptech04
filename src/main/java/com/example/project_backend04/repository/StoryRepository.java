package com.example.project_backend04.repository;

import com.example.project_backend04.entity.Story;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.enums.StoryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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


    @Query("SELECT s FROM Story s JOIN FETCH s.user WHERE s.status = 'PENDING' ORDER BY s.createdAt DESC")
    List<Story> findPendingStories();

    @Query("SELECT s FROM Story s JOIN FETCH s.user WHERE s.status = 'PENDING' ORDER BY s.createdAt DESC")
    Page<Story> findPendingStoriesPaginated(Pageable pageable);


    @Query("SELECT s FROM Story s JOIN FETCH s.user WHERE s.status = :status ORDER BY s.createdAt DESC")
    List<Story> findStoriesByStatus(@Param("status") StoryStatus status);

    @Query("SELECT s FROM Story s JOIN FETCH s.user WHERE s.status = :status ORDER BY s.createdAt DESC")
    Page<Story> findStoriesByStatusPaginated(@Param("status") StoryStatus status, Pageable pageable);

    @Query("SELECT COUNT(s) FROM Story s WHERE s.status = 'PENDING'")
    long countPendingStories();

    @Query("SELECT COUNT(s) FROM Story s WHERE s.status = 'APPROVED'")
    long countApprovedStories();

    @Query("SELECT COUNT(s) FROM Story s WHERE s.status = 'REJECTED'")
    long countRejectedStories();

    @Query("SELECT s FROM Story s JOIN FETCH s.user ORDER BY s.createdAt DESC")
    List<Story> findAllStoriesWithUser();

    @Query("SELECT s FROM Story s JOIN FETCH s.user ORDER BY s.createdAt DESC")
    Page<Story> findAllStoriesWithUserPaginated(Pageable pageable);

    /**
     * Find story by ID with user eagerly loaded
     */
    @Query("SELECT s FROM Story s JOIN FETCH s.user WHERE s.id = :id")
    Story findByIdWithUser(@Param("id") Long id);
}

