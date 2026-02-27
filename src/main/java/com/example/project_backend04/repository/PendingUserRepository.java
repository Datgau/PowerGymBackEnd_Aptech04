package com.example.project_backend04.repository;

import com.example.project_backend04.entity.PendingUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PendingUserRepository extends JpaRepository<PendingUser, Long> {


    boolean existsByEmail(String email);

    Optional<PendingUser> findByEmail(String email);

    @Modifying
    @Transactional
    void deleteAllByOtpExpiryBefore(LocalDateTime now);
}

