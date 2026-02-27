package com.example.project_backend04.repository;

import com.example.project_backend04.entity.GymService;
import com.example.project_backend04.enums.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GymServiceRepository extends JpaRepository<GymService, Long> {

    @Query("SELECT gs FROM GymService gs LEFT JOIN FETCH gs.images WHERE gs.isActive = true")
    List<GymService> findByIsActiveTrueWithImages();

    @Query("SELECT gs FROM GymService gs LEFT JOIN FETCH gs.images WHERE gs.id = :id")
    Optional<GymService> findByIdWithImages(@Param("id") Long id);

    @Query("SELECT gs FROM GymService gs LEFT JOIN FETCH gs.images")
    List<GymService> findAllWithImages();

    // Original methods (giữ lại để backward compatibility)
    List<GymService> findByIsActiveTrue();

    //tìm dịch vụ theo danh mục và đang hoạt động
    List<GymService> findByCategoryAndIsActiveTrue(ServiceCategory category);

    //tìm tất cả dịch vụ đang hoạt động và sắp xếp theo tên
    @Query("SELECT gs FROM GymService gs WHERE gs.isActive = true ORDER BY gs.name ASC")
    List<GymService> findAllActiveOrderByName();

    //tìm dịch vụ theo tên chứa chuỗi và đang hoạt động
    @Query("SELECT gs FROM GymService gs WHERE gs.name LIKE %:name% AND gs.isActive = true")
    List<GymService> findByNameContainingAndIsActiveTrue(@Param("name") String name);

    //tìm dịch vụ theo id và đang hoạt động
    Optional<GymService> findByIdAndIsActiveTrue(Long id);
}