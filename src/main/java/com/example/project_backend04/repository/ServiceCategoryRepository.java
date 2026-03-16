package com.example.project_backend04.repository;

import com.example.project_backend04.entity.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {

    // Tìm category theo name
    Optional<ServiceCategory> findByNameIgnoreCase(String name);

    // Tìm tất cả categories active, sắp xếp theo sortOrder
    @Query("SELECT sc FROM ServiceCategory sc WHERE sc.isActive = true ORDER BY sc.sortOrder ASC, sc.displayName ASC")
    List<ServiceCategory> findAllActiveOrderBySortOrder();

    // Tìm categories theo displayName
    @Query("SELECT sc FROM ServiceCategory sc WHERE sc.displayName LIKE %:displayName% AND sc.isActive = true")
    List<ServiceCategory> findByDisplayNameContainingIgnoreCaseAndIsActiveTrue(@Param("displayName") String displayName);

    // Kiểm tra name đã tồn tại chưa
    boolean existsByNameIgnoreCase(String name);

    // Đếm số categories active
    long countByIsActiveTrue();

    // Tìm category có sortOrder lớn nhất
    @Query("SELECT MAX(sc.sortOrder) FROM ServiceCategory sc WHERE sc.isActive = true")
    Integer findMaxSortOrder();

    // Tìm categories được sử dụng bởi trainers
    @Query("SELECT DISTINCT sc FROM ServiceCategory sc JOIN sc.trainerSpecialties ts WHERE ts.isActive = true AND sc.isActive = true")
    List<ServiceCategory> findCategoriesUsedByTrainers();

    // Tìm categories được sử dụng bởi gym services
    @Query("SELECT DISTINCT sc FROM ServiceCategory sc JOIN sc.gymServices gs WHERE gs.isActive = true AND sc.isActive = true")
    List<ServiceCategory> findCategoriesUsedByGymServices();
}