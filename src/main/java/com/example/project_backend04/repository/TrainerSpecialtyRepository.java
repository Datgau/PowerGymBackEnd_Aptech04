package com.example.project_backend04.repository;

import com.example.project_backend04.entity.TrainerSpecialty;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.enums.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainerSpecialtyRepository extends JpaRepository<TrainerSpecialty, Long> {

    // Tìm tất cả specialties của một user trainer
    @Query("SELECT ts FROM TrainerSpecialty ts WHERE ts.user = :user AND ts.isActive = true ORDER BY ts.specialty ASC")
    List<TrainerSpecialty> findByUserAndIsActiveTrueOrderBySpecialtyAsc(@Param("user") User user);

    // Tìm specialty cụ thể của user trainer
    @Query("SELECT ts FROM TrainerSpecialty ts WHERE ts.user = :user AND ts.specialty = :specialty AND ts.isActive = true")
    Optional<TrainerSpecialty> findByUserAndSpecialtyAndIsActiveTrue(@Param("user") User user, @Param("specialty") ServiceCategory specialty);

    // Tìm tất cả trainer có specialty cụ thể
    @Query("SELECT ts FROM TrainerSpecialty ts WHERE ts.specialty = :specialty AND ts.isActive = true AND ts.user.isActive = true AND ts.user.role.name = 'TRAINER'")
    List<TrainerSpecialty> findBySpecialtyAndIsActiveTrue(@Param("specialty") ServiceCategory specialty);

    // Đếm số specialty của user trainer
    long countByUserAndIsActiveTrue(User user);

    // Đếm số trainer có specialty cụ thể
    @Query("SELECT COUNT(DISTINCT ts.user) FROM TrainerSpecialty ts WHERE ts.specialty = :specialty AND ts.isActive = true AND ts.user.isActive = true AND ts.user.role.name = 'TRAINER'")
    long countTrainersBySpecialty(@Param("specialty") ServiceCategory specialty);

    // Xóa tất cả specialties của user trainer
    void deleteByUser(User user);

    // Tìm tất cả trainers có thể dạy một category cụ thể
    @Query("SELECT DISTINCT ts.user FROM TrainerSpecialty ts WHERE ts.specialty = :category AND ts.isActive = true AND ts.user.isActive = true AND ts.user.role.name = 'TRAINER'")
    List<User> findTrainersByCategory(@Param("category") ServiceCategory category);

    // Tìm tất cả specialties của trainers
    @Query("SELECT ts FROM TrainerSpecialty ts WHERE ts.isActive = true AND ts.user.isActive = true AND ts.user.role.name = 'TRAINER' ORDER BY ts.user.fullName ASC")
    List<TrainerSpecialty> findAllActiveTrainerSpecialties();
}