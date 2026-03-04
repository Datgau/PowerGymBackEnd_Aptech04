package com.example.project_backend04.repository;

import com.example.project_backend04.entity.Story;
import com.example.project_backend04.entity.StoryLike;
import com.example.project_backend04.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoryLikeRepository extends JpaRepository<StoryLike, Long> {

    /**
     * Find like by story and user
     */
    Optional<StoryLike> findByStoryAndUser(Story story, User user);

    /**
     * Check if user has liked a story
     */
    boolean existsByStoryAndUser(Story story, User user);

    /**
     * Count likes for a story
     */
    Long countByStory(Story story);

    /**
     * Find all likes by user
     */
    List<StoryLike> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Find users who liked a story
     */
    @Query("SELECT sl.user FROM StoryLike sl WHERE sl.story = :story ORDER BY sl.createdAt DESC")
    List<User> findUsersByStory(@Param("story") Story story);

    /**
     * Delete like by story and user
     */
    void deleteByStoryAndUser(Story story, User user);
}