package com.example.project_backend04.exception;

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
