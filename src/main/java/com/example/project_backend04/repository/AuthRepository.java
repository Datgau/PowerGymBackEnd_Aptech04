package com.example.project_backend04.repository;

import com.example.project_backend04.entity.Role;
import com.example.project_backend04.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByRefreshToken(String refreshToken);

    @Query("SELECT r FROM Role r WHERE r.name = :name")
    Optional<Role> findRoleByName(String name);
}
