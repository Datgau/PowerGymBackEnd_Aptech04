package com.example.project_backend04.repository;

import com.example.project_backend04.entity.CheckIn;
import com.example.project_backend04.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CheckInRepository extends JpaRepository<CheckIn, Long> {
    
    List<CheckIn> findByUserOrderByCheckInTimeDesc(User user);
    
    List<CheckIn> findByUserAndDate(User user, LocalDate date);
    
    @Query("SELECT c FROM CheckIn c WHERE c.user = :user AND c.date >= :startDate AND c.date <= :endDate ORDER BY c.checkInTime DESC")
    List<CheckIn> findByUserAndDateRange(@Param("user") User user, 
                                        @Param("startDate") LocalDate startDate, 
                                        @Param("endDate") LocalDate endDate);
    
    @Query("SELECT c FROM CheckIn c WHERE c.user = :user AND c.checkOutTime IS NULL ORDER BY c.checkInTime DESC")
    List<CheckIn> findActiveCheckIns(@Param("user") User user);
    
    @Query("SELECT c FROM CheckIn c WHERE c.date = :date ORDER BY c.checkInTime DESC")
    List<CheckIn> findByDate(@Param("date") LocalDate date);
    
    @Query("SELECT COUNT(c) FROM CheckIn c WHERE c.date = :date")
    Long countCheckInsForDate(@Param("date") LocalDate date);
    
    @Query("SELECT COUNT(DISTINCT c.user) FROM CheckIn c WHERE c.date = :date")
    Long countUniqueUsersForDate(@Param("date") LocalDate date);
    
    @Query("SELECT c FROM CheckIn c WHERE c.user = :user AND c.date = :date AND c.checkOutTime IS NULL")
    Optional<CheckIn> findActiveCheckInForUserAndDate(@Param("user") User user, @Param("date") LocalDate date);
    
    @Query("SELECT c FROM CheckIn c WHERE c.checkInTime >= :startTime AND c.checkInTime <= :endTime")
    List<CheckIn> findByCheckInTimeBetween(@Param("startTime") LocalDateTime startTime, 
                                          @Param("endTime") LocalDateTime endTime);
}