package com.example.project_backend04.exception;

/**
 * Exception thrown when a payment order is not found
 */
public class PaymentOrderNotFoundException extends BankPaymentException {
    
    private final String content;
    
    public PaymentOrderNotFoundException(String content) {
        super("Payment order not found with content: " + content, "PAYMENT_ORDER_NOT_FOUND");
        this.content = content;
    }
    
    public String getContent() {
        return content;
    }
}