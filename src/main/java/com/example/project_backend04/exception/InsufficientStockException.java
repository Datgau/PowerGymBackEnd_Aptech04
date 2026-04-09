package com.example.project_backend04.exception;

/**
 * Exception thrown when there is insufficient stock for a product order
 */
public class InsufficientStockException extends RuntimeException {
    
    private final Long productId;
    private final String productName;
    private final Integer requestedQuantity;
    private final Integer availableStock;
    
    public InsufficientStockException(Long productId, String productName, Integer requestedQuantity, Integer availableStock) {
        super(String.format("Insufficient stock for product '%s' (ID: %d). Requested: %d, Available: %d", 
            productName, productId, requestedQuantity, availableStock));
        this.productId = productId;
        this.productName = productName;
        this.requestedQuantity = requestedQuantity;
        this.availableStock = availableStock;
    }
    
    public Long getProductId() {
        return productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }
    
    public Integer getAvailableStock() {
        return availableStock;
    }
}
