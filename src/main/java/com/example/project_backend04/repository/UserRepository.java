package com.example.project_backend04.repository;

import com.example.project_backend04.entity.Role;
import com.example.project_backend04.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    
    // Tìm users theo role và trạng thái active
    Page<User> findByRoleAndIsActiveTrue(Role role, Pageable pageable);
    
    // Tìm tất cả users theo role
    List<User> findByRoleAndIsActiveTrue(Role role);
    
    // Đếm số users theo role
    long countByRoleAndIsActiveTrue(Role role);
    
    // Tìm users theo role name
    @Query("SELECT u FROM User u WHERE u.role.name = :roleName AND u.isActive = true")
    List<User> findByRoleNameAndIsActiveTrue(@Param("roleName") String roleName);
}
