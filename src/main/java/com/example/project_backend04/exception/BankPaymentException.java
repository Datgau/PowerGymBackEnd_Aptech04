package com.example.project_backend04.exception;

/**
 * Base exception for bank payment operations
 */
public class BankPaymentException extends RuntimeException {
    
    private final String errorCode;
    
    public BankPaymentException(String message) {
        super(message);
        this.errorCode = "BANK_PAYMENT_ERROR";
    }
    
    public BankPaymentException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public BankPaymentException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "BANK_PAYMENT_ERROR";
    }
    
    public BankPaymentException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}