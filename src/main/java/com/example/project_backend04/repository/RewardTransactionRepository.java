package com.example.project_backend04.repository;

import com.example.project_backend04.entity.RewardTransaction;
import com.example.project_backend04.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RewardTransactionRepository extends JpaRepository<RewardTransaction, Long> {
    
    @Query("SELECT rt FROM RewardTransaction rt WHERE rt.userId = :userId ORDER BY rt.createdAt DESC")
    List<RewardTransaction> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    @Query("SELECT rt FROM RewardTransaction rt WHERE rt.userId = :userId ORDER BY rt.createdAt DESC")
    Page<RewardTransaction> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT rt FROM RewardTransaction rt WHERE rt.userId = :userId AND rt.transactionType = :transactionType ORDER BY rt.createdAt DESC")
    List<RewardTransaction> findByUserIdAndTransactionTypeOrderByCreatedAtDesc(
        @Param("userId") Long userId, 
        @Param("transactionType") TransactionType transactionType);
    
    @Query("SELECT SUM(rt.points) FROM RewardTransaction rt " +
           "WHERE rt.userId = :userId AND rt.transactionType = :type " +
           "AND rt.createdAt BETWEEN :startDate AND :endDate")
    Integer sumPointsByUserIdAndTypeAndDateRange(
        @Param("userId") Long userId,
        @Param("type") TransactionType type,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
