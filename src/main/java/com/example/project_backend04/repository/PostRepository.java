package com.example.project_backend04.repository;

import com.example.project_backend04.entity.Post;
import com.example.project_backend04.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByUserOrderByCreatedAtDesc(User user);
    
    /**
     * Tìm tất cả posts với eager loading user, sắp xếp theo thời gian mới nhất
     */
    @Query("SELECT DISTINCT p FROM Post p " +
           "LEFT JOIN FETCH p.user " +
           "ORDER BY p.createdAt DESC")
    List<Post> findAllByOrderByCreatedAtDesc();
    
    /**
     * Tìm post theo ID với eager loading user
     */
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN FETCH p.user " +
           "WHERE p.id = :id")
    Optional<Post> findByIdWithDetails(Long id);
    
    /**
     * Đếm số lượng posts của user
     */
    long countByUser(User user);
}
