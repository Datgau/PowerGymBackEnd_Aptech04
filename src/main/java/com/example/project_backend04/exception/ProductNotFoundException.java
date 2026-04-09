package com.example.project_backend04.exception;

/**
 * Exception thrown when a product is not found
 */
public class ProductNotFoundException extends RuntimeException {
    
    private final Long productId;
    
    public ProductNotFoundException(Long productId) {
        super("Product not found with id: " + productId);
        this.productId = productId;
    }
    
    public Long getProductId() {
        return productId;
    }
}
