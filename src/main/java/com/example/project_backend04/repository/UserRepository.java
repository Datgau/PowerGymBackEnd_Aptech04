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

    // Tìm tất cả users theo role (bao gồm cả inactive)
    Page<User> findByRole(Role role, Pageable pageable);
    

    // Tìm tất cả users theo role với ordering
    List<User> findByRoleAndIsActiveTrueOrderByCreateDateDesc(Role role);

    // Tìm users theo multiple role names với pagination
    @Query("SELECT u FROM User u WHERE u.role.name IN :roleNames ORDER BY u.createDate DESC")
    Page<User> findByRoleNameIn(@Param("roleNames") List<String> roleNames, Pageable pageable);
    
    // Đếm số lượng users theo role name
    @Query("SELECT COUNT(u) FROM User u WHERE u.role.name = :roleName")
    long countByRoleName(@Param("roleName") String roleName);
    
    // Tìm users theo single role name với pagination
    @Query("SELECT u FROM User u WHERE u.role.name = :roleName ORDER BY u.createDate DESC")
    Page<User> findByRoleName(@Param("roleName") String roleName, Pageable pageable);
    
    // Search users theo email hoặc phone trong tất cả roles (USER, STAFF)
    @Query("SELECT u FROM User u WHERE u.role.name IN :roleNames AND " +
           "(u.email LIKE CONCAT('%', :searchTerm, '%') OR " +
           "u.phoneNumber LIKE CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY u.createDate DESC")
    Page<User> searchByEmailOrPhoneInRoles(@Param("roleNames") List<String> roleNames, 
                                          @Param("searchTerm") String searchTerm, 
                                          Pageable pageable);
    
    // Search users theo email hoặc phone trong single role
    @Query("SELECT u FROM User u WHERE u.role.name = :roleName AND " +
           "(u.email LIKE CONCAT('%', :searchTerm, '%') OR " +
           "u.phoneNumber LIKE CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY u.createDate DESC")
    Page<User> searchByEmailOrPhoneInRole(@Param("roleName") String roleName, 
                                         @Param("searchTerm") String searchTerm, 
                                         Pageable pageable);
}
