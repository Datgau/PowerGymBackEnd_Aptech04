package com.example.project_backend04.repository;

import com.example.project_backend04.entity.EquipmentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentCategoryRepository extends JpaRepository<EquipmentCategory, Long> {
    List<EquipmentCategory> findByIsActiveTrue();
    Optional<EquipmentCategory> findByName(String name);
    boolean existsByName(String name);
}