package com.example.project_backend04.repository;

import com.example.project_backend04.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByUserIdAndIsUsedFalse(Long userId);

    List<PasswordResetToken> findAllByIsUsedFalse();
}