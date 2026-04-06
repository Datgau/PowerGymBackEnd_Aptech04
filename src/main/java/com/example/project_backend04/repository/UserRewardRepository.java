package com.example.project_backend04.repository;

import com.example.project_backend04.entity.UserReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface UserRewardRepository extends JpaRepository<UserReward, Long> {
    
    @Query("SELECT ur FROM UserReward ur WHERE ur.userId = :userId")
    Optional<UserReward> findByUserId(@Param("userId") Long userId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ur FROM UserReward ur WHERE ur.userId = :userId")
    Optional<UserReward> findByUserIdWithLock(@Param("userId") Long userId);
    
    @Query("SELECT CASE WHEN COUNT(ur) > 0 THEN true ELSE false END FROM UserReward ur WHERE ur.userId = :userId")
    boolean existsByUserId(@Param("userId") Long userId);
}
