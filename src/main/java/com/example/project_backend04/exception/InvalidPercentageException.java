package com.example.project_backend04.exception;

public class InvalidPercentageException extends RuntimeException {
    
    public InvalidPercentageException() {
        super("Trainer percentage must be between 0.0 and 1.0");
    }
}
