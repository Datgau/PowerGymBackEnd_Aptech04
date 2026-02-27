package com.example.project_backend04.repository;

import com.example.project_backend04.entity.UserProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProviderRepository extends JpaRepository<UserProvider, Long> {
    Optional<UserProvider> findByProviderAndProviderUserId(String provider, String providerUserId);
}
