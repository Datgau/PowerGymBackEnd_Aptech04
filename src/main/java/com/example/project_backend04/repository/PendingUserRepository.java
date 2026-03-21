package com.example.project_backend04.repository;

import com.example.project_backend04.entity.PendingUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PendingUserRepository extends JpaRepository<PendingUser, Long> {

    Optional<PendingUser> findByEmail(String email);

    @Modifying
    @Transactional
    void deleteAllByOtpExpiryBefore(LocalDateTime now);

    @Modifying
    @Transactional
    void deleteByEmail(String email);

    @Modifying
    @Transactional
    void deleteAllByCreatedAtBefore(LocalDateTime cutoffTime);

    // Update existing PendingUser với thông tin mới
    @Modifying
    @Transactional
    @Query("UPDATE PendingUser p SET p.password = :password, p.fullName = :fullName, p.otp = :otp, p.otpExpiry = :otpExpiry WHERE p.email = :email")
    int updatePendingUserByEmail(@Param("email") String email, 
                                @Param("password") String password, 
                                @Param("fullName") String fullName, 
                                @Param("otp") String otp, 
                                @Param("otpExpiry") LocalDateTime otpExpiry);
}

