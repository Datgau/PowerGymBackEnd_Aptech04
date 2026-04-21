package com.example.project_backend04.exception;


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
