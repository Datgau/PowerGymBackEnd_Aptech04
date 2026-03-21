package com.example.project_backend04.repository;

import com.example.project_backend04.entity.ServiceCategory;
import com.example.project_backend04.entity.TrainerSpecialty;
import com.example.project_backend04.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainerSpecialtyRepository extends JpaRepository<TrainerSpecialty, Long> {

    // Tìm tất cả specialties của một user trainer
    @Query("SELECT ts FROM TrainerSpecialty ts JOIN FETCH ts.specialty WHERE ts.user = :user AND ts.isActive = true ORDER BY ts.specialty.displayName ASC")
    List<TrainerSpecialty> findByUserAndIsActiveTrueOrderBySpecialtyAsc(@Param("user") User user);

    // Tìm specialty cụ thể của user trainer
    @Query("SELECT ts FROM TrainerSpecialty ts JOIN FETCH ts.specialty WHERE ts.user = :user AND ts.specialty = :specialty AND ts.isActive = true")
    Optional<TrainerSpecialty> findByUserAndSpecialtyAndIsActiveTrue(@Param("user") User user, @Param("specialty") ServiceCategory specialty);

    // Tìm tất cả trainer có specialty cụ thể
    @Query("SELECT ts FROM TrainerSpecialty ts JOIN FETCH ts.specialty WHERE ts.specialty = :specialty AND ts.isActive = true AND ts.user.isActive = true AND ts.user.role.name = 'TRAINER'")
    List<TrainerSpecialty> findBySpecialtyAndIsActiveTrue(@Param("specialty") ServiceCategory specialty);

    // Đếm số specialty của user trainer
    long countByUserAndIsActiveTrue(User user);

    // Đếm số trainer có specialty cụ thể
    @Query("SELECT COUNT(DISTINCT ts.user) FROM TrainerSpecialty ts WHERE ts.specialty = :specialty AND ts.isActive = true AND ts.user.isActive = true AND ts.user.role.name = 'TRAINER'")
    long countTrainersBySpecialty(@Param("specialty") ServiceCategory specialty);

    // Đếm số specialty của user trainer cụ thể
    @Query("SELECT COUNT(ts) FROM TrainerSpecialty ts WHERE ts.specialty = :specialty AND ts.isActive = true")
    long countBySpecialtyAndIsActiveTrue(@Param("specialty") ServiceCategory specialty);
    // Xóa tất cả specialties của user trainer
    void deleteByUser(User user);

    // Tìm tất cả trainers có thể dạy một category cụ thể
    @Query("SELECT DISTINCT ts.user FROM TrainerSpecialty ts WHERE ts.specialty = :category AND ts.isActive = true AND ts.user.isActive = true AND ts.user.role.name = 'TRAINER'")
    List<User> findTrainersByCategory(@Param("category") ServiceCategory category);

    // Tìm tất cả specialties của trainers
    @Query("SELECT ts FROM TrainerSpecialty ts JOIN FETCH ts.specialty WHERE ts.isActive = true AND ts.user.isActive = true AND ts.user.role.name = 'TRAINER' ORDER BY ts.user.fullName ASC")
    List<TrainerSpecialty> findAllActiveTrainerSpecialties();
    
    // NEW METHODS for trainer selection
    
    /**
     * Find trainers by specialty category ID
     */
    @Query("SELECT DISTINCT ts.user FROM TrainerSpecialty ts " +
           "WHERE ts.specialty.id = :categoryId " +
           "AND ts.isActive = true " +
           "AND ts.user.isActive = true " +
           "AND ts.user.role.name = 'TRAINER'")
    List<User> findTrainersBySpecialtyCategory(@Param("categoryId") Long categoryId);
    
    /**
     * Find active specialties by trainer with specialty details
     */
    @Query("SELECT ts FROM TrainerSpecialty ts " +
           "JOIN FETCH ts.specialty " +
           "WHERE ts.user.id = :trainerId AND ts.isActive = true")
    List<TrainerSpecialty> findActiveSpecialtiesByTrainer(@Param("trainerId") Long trainerId);
    
    /**
     * Count trainers by specialty category
     */
    @Query("SELECT COUNT(DISTINCT ts.user) FROM TrainerSpecialty ts " +
           "WHERE ts.specialty.id = :categoryId AND ts.isActive = true " +
           "AND ts.user.isActive = true AND ts.user.role.name = 'TRAINER'")
    Long countTrainersBySpecialtyCategory(@Param("categoryId") Long categoryId);
    
    /**
     * Find trainers with multiple specialties including specific category
     */
    @Query("SELECT DISTINCT ts.user FROM TrainerSpecialty ts " +
           "WHERE ts.specialty.id = :categoryId " +
           "AND ts.isActive = true " +
           "AND ts.user.isActive = true " +
           "AND ts.user.role.name = 'TRAINER' " +
           "AND ts.user.id IN (" +
           "    SELECT ts2.user.id FROM TrainerSpecialty ts2 " +
           "    WHERE ts2.isActive = true " +
           "    GROUP BY ts2.user.id " +
           "    HAVING COUNT(ts2) >= :minSpecialties" +
           ")")
    List<User> findExperiencedTrainersByCategory(
        @Param("categoryId") Long categoryId, 
        @Param("minSpecialties") Long minSpecialties);
    
    /**
     * Find trainer specialties with experience and certification info
     */
    @Query("SELECT ts FROM TrainerSpecialty ts " +
           "JOIN FETCH ts.specialty " +
           "JOIN FETCH ts.user " +
           "WHERE ts.specialty.id = :categoryId " +
           "AND ts.isActive = true " +
           "AND ts.user.isActive = true " +
           "AND ts.user.role.name = 'TRAINER' " +
           "ORDER BY ts.experienceYears DESC NULLS LAST, ts.level DESC")
    List<TrainerSpecialty> findTrainerSpecialtiesByCategory(@Param("categoryId") Long categoryId);
    
    /**
     * Check if trainer has specialty for specific category
     */
    @Query("SELECT COUNT(ts) > 0 FROM TrainerSpecialty ts " +
           "WHERE ts.user.id = :trainerId " +
           "AND ts.specialty.id = :categoryId " +
           "AND ts.isActive = true")
    boolean hasTrainerSpecialtyForCategory(@Param("trainerId") Long trainerId, @Param("categoryId") Long categoryId);
    
    /**
     * Find trainers by multiple categories (OR condition)
     */
    @Query("SELECT DISTINCT ts.user FROM TrainerSpecialty ts " +
           "WHERE ts.specialty.id IN :categoryIds " +
           "AND ts.isActive = true " +
           "AND ts.user.isActive = true " +
           "AND ts.user.role.name = 'TRAINER'")
    List<User> findTrainersByMultipleCategories(@Param("categoryIds") List<Long> categoryIds);
    
    /**
     * Find top-rated trainers by category based on experience
     */
    @Query("SELECT ts FROM TrainerSpecialty ts " +
           "JOIN FETCH ts.specialty " +
           "JOIN FETCH ts.user " +
           "WHERE ts.specialty.id = :categoryId " +
           "AND ts.isActive = true " +
           "AND ts.user.isActive = true " +
           "AND ts.user.role.name = 'TRAINER' " +
           "ORDER BY " +
           "CASE WHEN ts.level = 'EXPERT' THEN 4 " +
           "     WHEN ts.level = 'ADVANCED' THEN 3 " +
           "     WHEN ts.level = 'INTERMEDIATE' THEN 2 " +
           "     WHEN ts.level = 'BEGINNER' THEN 1 " +
           "     ELSE 0 END DESC, " +
           "ts.experienceYears DESC NULLS LAST")
    List<TrainerSpecialty> findTopTrainersByCategory(@Param("categoryId") Long categoryId);
}