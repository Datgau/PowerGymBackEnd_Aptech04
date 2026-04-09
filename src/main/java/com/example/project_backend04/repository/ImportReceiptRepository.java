package com.example.project_backend04.repository;

import com.example.project_backend04.entity.ImportReceipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ImportReceiptRepository extends JpaRepository<ImportReceipt, Long>,
                                                    JpaSpecificationExecutor<ImportReceipt> {

    List<ImportReceipt> findBySupplierNameContainingIgnoreCase(String supplierName);

    List<ImportReceipt> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("""
    SELECT ir FROM ImportReceipt ir WHERE
    (:startDate IS NULL OR ir.createdAt >= :startDate) AND
    (:endDate IS NULL OR ir.createdAt <= :endDate) AND
    (:supplierName IS NULL OR LOWER(ir.supplierName) LIKE LOWER(CONCAT('%', CAST(:supplierName AS string), '%')))
    ORDER BY ir.createdAt DESC
""")
    Page<ImportReceipt> findByFilters(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("supplierName") String supplierName,
            Pageable pageable
    );
}
