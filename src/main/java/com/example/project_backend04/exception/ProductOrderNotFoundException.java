package com.example.project_backend04.exception;

/**
 * Exception thrown when a product order is not found
 */
public class ProductOrderNotFoundException extends RuntimeException {
    
    private final Long orderId;
    
    public ProductOrderNotFoundException(Long orderId) {
        super("Product order not found with id: " + orderId);
        this.orderId = orderId;
    }
    
    public Long getOrderId() {
        return orderId;
    }
}
