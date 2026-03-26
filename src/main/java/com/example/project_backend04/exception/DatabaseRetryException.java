package com.example.project_backend04.exception;

/**
 * Exception thrown when database operations fail after retry attempts
 */
public class DatabaseRetryException extends BankPaymentException {
    
    private final int maxRetries;
    private final int attemptsMade;
    
    public DatabaseRetryException(String operation, int maxRetries, int attemptsMade, Throwable cause) {
        super("Database operation '" + operation + "' failed after " + attemptsMade + " attempts (max: " + maxRetries + ")", 
              "DATABASE_RETRY_FAILED", cause);
        this.maxRetries = maxRetries;
        this.attemptsMade = attemptsMade;
    }
    
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public int getAttemptsMade() {
        return attemptsMade;
    }
}