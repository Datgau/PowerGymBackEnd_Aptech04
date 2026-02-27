package com.example.project_backend04.repository;

import com.example.project_backend04.entity.Like;
import com.example.project_backend04.entity.LikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface LikeRepository extends JpaRepository<Like, LikeId> {
    
    @Query("SELECT COUNT(l) FROM Like l WHERE l.post.id = :postId")
    int countByPostId(Long postId);
}
