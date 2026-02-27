package com.example.project_backend04.repository;

import com.example.project_backend04.entity.Instructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstructorRepository extends JpaRepository<Instructor, Long> {
    
    List<Instructor> findByIsActiveTrue();
    
    @Query("SELECT i FROM Instructor i WHERE i.isActive = true ORDER BY i.name ASC")
    List<Instructor> findAllActiveOrderByName();
    
    @Query("SELECT i FROM Instructor i WHERE i.name LIKE %:name% AND i.isActive = true")
    List<Instructor> findByNameContainingAndIsActiveTrue(@Param("name") String name);
    
    Optional<Instructor> findByEmailAndIsActiveTrue(String email);
    
    Optional<Instructor> findByIdAndIsActiveTrue(Long id);
    
    @Query("SELECT i FROM Instructor i JOIN i.specializations s WHERE s = :specialization AND i.isActive = true")
    List<Instructor> findBySpecialization(@Param("specialization") String specialization);
}