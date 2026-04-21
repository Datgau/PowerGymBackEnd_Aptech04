package com.example.project_backend04.service;

import com.example.project_backend04.dto.response.Product.ProductResponse;
import com.example.project_backend04.dto.response.Product.ProductStatisticsResponse;
import com.example.project_backend04.dto.response.Product.TopSellingProductResponse;
import com.example.project_backend04.entity.Product;
import com.example.project_backend04.repository.ImportReceiptRepository;
import com.example.project_backend04.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for inventory statistics and reporting
 * Validates: Requirements 11.1, 11.2, 11.4, 12.1, 12.2
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryStatisticsService {
    
    private final ProductRepository productRepository;
    private final ImportReceiptRepository importReceiptRepository;

    @Transactional(readOnly = true)
    public ProductStatisticsResponse getProductStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        List<Product> allProducts = productRepository.findAll();
        int totalProducts = allProducts.size();

        int inStockCount = 0;
        int lowStockCount = 0;
        int outOfStockCount = 0;
        List<Product> lowStockProducts = new ArrayList<>();
        
        for (Product product : allProducts) {
            if (product.isOutOfStock()) {
                outOfStockCount++;
            } else if (product.isLowStock()) {
                lowStockCount++;
                lowStockProducts.add(product);
            } else {
                inStockCount++;
            }
        }

        List<ProductResponse> lowStockProductList = lowStockProducts.stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());

        List<TopSellingProductResponse> topSellingProducts = new ArrayList<>();
        
        return ProductStatisticsResponse.builder()
                .totalProducts(totalProducts)
                .inStockProducts(inStockCount)
                .lowStockProducts(lowStockCount)
                .outOfStockProducts(outOfStockCount)
                .lowStockProductList(lowStockProductList)
                .topSellingProducts(topSellingProducts)
                .build();
    }
    

    @Transactional(readOnly = true)
    public BigDecimal calculateTotalImportValue(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Calculating total import value for date range: {} to {}", startDate, endDate);
        
        return importReceiptRepository.findByCreatedAtBetween(startDate, endDate).stream()
                .map(receipt -> receipt.getTotalCost())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ProductResponse mapToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .stock(product.getStock())
                .lowStockThreshold(product.getLowStockThreshold())
                .lowStock(product.isLowStock())
                .outOfStock(product.isOutOfStock())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
