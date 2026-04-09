package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.CreateProductRequest;
import com.example.project_backend04.dto.request.UpdateProductRequest;
import com.example.project_backend04.dto.response.Product.ProductResponse;
import com.example.project_backend04.entity.Product;
import com.example.project_backend04.exception.ProductNotFoundException;
import com.example.project_backend04.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    
    private final ProductRepository productRepository;
    
    /**
     * Create a new product
     * Validates: Requirements 1.1
     */
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Creating new product: {}", request.getName());
        
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setStock(request.getStock());
        product.setLowStockThreshold(request.getLowStockThreshold() != null ? 
            request.getLowStockThreshold() : 10);
        
        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with id: {}", savedProduct.getId());
        
        return mapToResponse(savedProduct);
    }
    
    /**
     * Update an existing product
     * Validates: Requirements 1.2
     */
    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        log.info("Updating product with id: {}", id);
        
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        
        if (request.getLowStockThreshold() != null) {
            product.setLowStockThreshold(request.getLowStockThreshold());
        }
        
        Product updatedProduct = productRepository.save(product);
        log.info("Product updated successfully: {}", updatedProduct.getId());
        
        return mapToResponse(updatedProduct);
    }
    
    /**
     * Delete a product
     * Validates: Requirements 1.3, 1.4 - prevents deletion if product has order items
     */
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Attempting to delete product with id: {}", id);
        
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        
        // Check if product has associated order items
        if (product.getProductOrderItems() != null && !product.getProductOrderItems().isEmpty()) {
            log.error("Cannot delete product {} - has {} associated order items", 
                id, product.getProductOrderItems().size());
            throw new IllegalStateException(
                "Cannot delete product with associated order items. Product has " + 
                product.getProductOrderItems().size() + " order item(s).");
        }
        
        productRepository.delete(product);
        log.info("Product deleted successfully: {}", id);
    }
    
    /**
     * Get product by ID
     * Validates: Requirements 2.5
     */
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        log.info("Fetching product with id: {}", id);
        
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        
        return mapToResponse(product);
    }
    
    /**
     * Get all products with pagination, search, and filtering
     * Validates: Requirements 2.1, 2.3, 2.4, 2.6
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(int page, int size, String search, String stockStatus) {
        log.info("Fetching products - page: {}, size: {}, search: {}, stockStatus: {}", 
            page, size, search, stockStatus);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> products;
        
        boolean hasSearch = search != null && !search.trim().isEmpty();
        boolean hasStockFilter = stockStatus != null && !stockStatus.trim().isEmpty() && !stockStatus.equalsIgnoreCase("all");
        
        // Determine which query to use based on search and filter parameters
        if (hasSearch && hasStockFilter) {
            // Both search and stock filter
            String searchTerm = search.trim();
            switch (stockStatus.toLowerCase()) {
                case "in_stock":
                    products = productRepository.searchInStockProducts(searchTerm, pageable);
                    break;
                case "low_stock":
                    products = productRepository.searchLowStockProducts(searchTerm, pageable);
                    break;
                case "out_of_stock":
                    products = productRepository.searchOutOfStockProducts(searchTerm, pageable);
                    break;
                default:
                    products = productRepository.searchProducts(searchTerm, pageable);
            }
        } else if (hasSearch) {
            // Only search, no stock filter
            products = productRepository.searchProducts(search.trim(), pageable);
        } else if (hasStockFilter) {
            // Only stock filter, no search
            switch (stockStatus.toLowerCase()) {
                case "in_stock":
                    products = productRepository.findByStockGreaterThan(0, pageable);
                    break;
                case "low_stock":
                    products = productRepository.findLowStockProducts(pageable);
                    break;
                case "out_of_stock":
                    products = productRepository.findOutOfStockProducts(pageable);
                    break;
                default:
                    products = productRepository.findAll(pageable);
            }
        } else {
            // No filters, return all products
            products = productRepository.findAll(pageable);
        }
        
        return products.map(this::mapToResponse);
    }
    
    /**
     * Map Product entity to ProductResponse DTO
     */
    private ProductResponse mapToResponse(Product product) {
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
