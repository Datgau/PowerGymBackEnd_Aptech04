package com.example.project_backend04.controller;

import com.example.project_backend04.dto.response.Product.OrderStatisticsResponse;
import com.example.project_backend04.dto.response.Product.ProductStatisticsResponse;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.service.InventoryStatisticsService;
import com.example.project_backend04.service.OrderStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * REST Controller for Statistics and Reporting
 * Provides product and order statistics with admin-only access
 */
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Slf4j
public class StatisticsController {
    
    private final InventoryStatisticsService inventoryStatisticsService;
    private final OrderStatisticsService orderStatisticsService;

    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<ProductStatisticsResponse>> getProductStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {

        ProductStatisticsResponse statistics = inventoryStatisticsService.getProductStatistics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(statistics, "Product statistics retrieved successfully"));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<OrderStatisticsResponse>> getOrderStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        LocalDateTime effectiveStartDate = startDate != null ? startDate : LocalDateTime.now().minusDays(30);
        LocalDateTime effectiveEndDate = endDate != null ? endDate : LocalDateTime.now();
        
        OrderStatisticsResponse statistics = orderStatisticsService.getOrderStatistics(
                effectiveStartDate, effectiveEndDate
        );
        return ResponseEntity.ok(ApiResponse.success(statistics, "Order statistics retrieved successfully"));
    }
}
