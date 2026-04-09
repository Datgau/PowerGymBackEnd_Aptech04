package com.example.project_backend04.exception;

import com.example.project_backend04.entity.PaymentStatus;

/**
 * Exception thrown when an invalid payment status transition is attempted
 */
public class InvalidStatusTransitionException extends RuntimeException {
    
    private final PaymentStatus currentStatus;
    private final PaymentStatus requestedStatus;
    
    public InvalidStatusTransitionException(PaymentStatus currentStatus, PaymentStatus requestedStatus) {
        super(String.format("Invalid payment status transition from %s to %s", currentStatus, requestedStatus));
        this.currentStatus = currentStatus;
        this.requestedStatus = requestedStatus;
    }
    
    public PaymentStatus getCurrentStatus() {
        return currentStatus;
    }
    
    public PaymentStatus getRequestedStatus() {
        return requestedStatus;
    }
}
