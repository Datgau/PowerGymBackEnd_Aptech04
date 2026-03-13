package com.example.project_backend04.repository;

import com.example.project_backend04.entity.Equipment;
import com.example.project_backend04.entity.EquipmentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    List<Equipment> findByIsActiveTrue();
    List<Equipment> findByCategory(EquipmentCategory category);
    List<Equipment> findByCategoryAndIsActiveTrue(EquipmentCategory category);
    List<Equipment> findByCategoryId(Long categoryId);
    List<Equipment> findByCategoryIdAndIsActiveTrue(Long categoryId);
}