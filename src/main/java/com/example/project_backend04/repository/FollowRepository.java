package com.example.project_backend04.repository;

import com.example.project_backend04.entity.Follow;
import com.example.project_backend04.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    boolean existsByFollower_IdAndFollowing_Id(Long followerId, Long followingId);


    /**
     * Lấy ID user mà followerId follow
     */
    @Query("""
        SELECT f.following.id
        FROM Follow f
        WHERE f.follower.id = :followerId
    """)
    List<Long> findFollowingIds(Long followerId);

    /**
     * Lấy danh sách user mutual follow
     */
    @Query("""
        SELECT u
        FROM User u
        WHERE u.id IN (
            SELECT f1.following.id
            FROM Follow f1
            WHERE f1.follower.id = :userId
        )
        AND u.id IN (
            SELECT f2.follower.id
            FROM Follow f2
            WHERE f2.following.id = :userId
        )
    """)
    List<User> findMutualFollowUsers(Long userId);

    // Xoá follow
    void deleteByFollower_IdAndFollowing_Id(Long followerId, Long followingId);
}
