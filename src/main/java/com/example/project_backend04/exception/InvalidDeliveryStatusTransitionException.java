package com.example.project_backend04.exception;

import com.example.project_backend04.enums.DeliveryStatus;

/**
 * Exception thrown when an invalid delivery status transition is attempted
 */
public class InvalidDeliveryStatusTransitionException extends RuntimeException {
    
    private final DeliveryStatus currentStatus;
    private final DeliveryStatus requestedStatus;
    
    public InvalidDeliveryStatusTransitionException(DeliveryStatus currentStatus, DeliveryStatus requestedStatus) {
        super(String.format("Invalid delivery status transition from %s to %s", currentStatus, requestedStatus));
        this.currentStatus = currentStatus;
        this.requestedStatus = requestedStatus;
    }
    
    public DeliveryStatus getCurrentStatus() {
        return currentStatus;
    }
    
    public DeliveryStatus getRequestedStatus() {
        return requestedStatus;
    }
}
