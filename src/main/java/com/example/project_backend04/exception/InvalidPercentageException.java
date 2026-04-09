package com.example.project_backend04.exception;

/**
 * Exception thrown when trainer percentage is outside valid range (0.0 to 1.0)
 */
public class InvalidPercentageException extends RuntimeException {
    
    public InvalidPercentageException() {
        super("Trainer percentage must be between 0.0 and 1.0");
    }
}
