package com.example.project_backend04.controller;

import com.example.project_backend04.dto.request.CreateProductRequest;
import com.example.project_backend04.dto.request.UpdateProductRequest;
import com.example.project_backend04.dto.response.Product.ProductResponse;
import com.example.project_backend04.dto.response.Product.ProductStatisticsResponse;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.service.CloudinaryService;
import com.example.project_backend04.service.InventoryStatisticsService;
import com.example.project_backend04.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {
    
    private final ProductService productService;
    private final CloudinaryService cloudinaryService;
    private final InventoryStatisticsService inventoryStatisticsService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String stockStatus
    ) {
        Page<ProductResponse> products = productService.getAllProducts(page, size, search, stockStatus);
        return ResponseEntity.ok(ApiResponse.success(products, "Products retrieved successfully"));
    }
    
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<ProductStatisticsResponse>> getProductStatistics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") java.time.LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") java.time.LocalDate endDate
    ) {
        log.info("GET /api/products/statistics - startDate: {}, endDate: {}", startDate, endDate);
        LocalDateTime effectiveStartDate = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime effectiveEndDate = endDate != null ? endDate.atTime(23, 59, 59) : null;
        ProductStatisticsResponse statistics = inventoryStatisticsService.getProductStatistics(effectiveStartDate, effectiveEndDate);
        return ResponseEntity.ok(ApiResponse.success(statistics, "Product statistics retrieved successfully"));
    }
    

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {

        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product, "Product retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request
    ) {
        ProductResponse product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(product, "Product created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        log.info("PUT /api/products/{} - Updating product", id);
        
        ProductResponse product = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success(product, "Product updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        log.info("DELETE /api/products/{}", id);
        
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Product deleted successfully"));
    }

    @PostMapping("/{id}/image")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<ProductResponse>> uploadProductImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        ProductResponse product = productService.getProductById(id);
        String imageUrl = cloudinaryService.uploadSingleFile(file, "products");

        // Update product with new image URL
        UpdateProductRequest updateRequest = new UpdateProductRequest();
        updateRequest.setName(product.getName());
        updateRequest.setDescription(product.getDescription());
        updateRequest.setPrice(product.getPrice());
        updateRequest.setImageUrl(imageUrl);
        updateRequest.setLowStockThreshold(product.getLowStockThreshold());

        ProductResponse updatedProduct = productService.updateProduct(id, updateRequest);

        return ResponseEntity.ok(
                ApiResponse.success(updatedProduct, "Product image uploaded successfully")
        );
    }
}
