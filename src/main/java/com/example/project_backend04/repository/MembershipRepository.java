package com.example.project_backend04.repository;

import com.example.project_backend04.entity.Membership;
import com.example.project_backend04.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long> {
    
    @Query("SELECT m FROM Membership m WHERE m.user = :user AND m.status = 'ACTIVE' AND m.endDate > :currentDate ORDER BY m.endDate DESC")
    Optional<Membership> findCurrentActiveMembership(@Param("user") User user, @Param("currentDate") LocalDate currentDate);
    
    List<Membership> findByUserOrderByCreateDateDesc(User user);
    
    List<Membership> findByUserAndStatus(User user, Membership.MembershipStatus status);
    
    @Query("SELECT m FROM Membership m WHERE m.endDate = :date AND m.status = 'ACTIVE'")
    List<Membership> findExpiringMemberships(@Param("date") LocalDate date);
    
    @Query("SELECT m FROM Membership m WHERE m.endDate < :date AND m.status = 'ACTIVE'")
    List<Membership> findExpiredMemberships(@Param("date") LocalDate date);
    
    @Query("SELECT COUNT(m) FROM Membership m WHERE m.status = 'ACTIVE' AND m.endDate > :currentDate")
    Long countActiveMemberships(@Param("currentDate") LocalDate currentDate);
    
    Optional<Membership> findByOrderId(String orderId);
}