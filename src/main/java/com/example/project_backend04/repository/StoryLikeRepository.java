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


}