package com.example.project_backend04.repository;

import com.example.project_backend04.entity.MembershipPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipPackageRepository extends JpaRepository<MembershipPackage, Long> {
    
    List<MembershipPackage> findByIsActiveTrue();
    
    @Query("SELECT mp FROM MembershipPackage mp WHERE mp.isActive = true ORDER BY mp.price ASC")
    List<MembershipPackage> findAllActiveOrderByPrice();
    
    Optional<MembershipPackage> findByPackageIdAndIsActiveTrue(String packageId);
    
    Optional<MembershipPackage> findByIdAndIsActiveTrue(Long id);
    
    List<MembershipPackage> findByIsPopularTrueAndIsActiveTrue();
}