package com.example.project_backend04.exception;

import com.example.project_backend04.dto.response.TrainerBooking.ConflictCheckResult;

public class BookingConflictException extends RuntimeException {
    
    private final ConflictCheckResult conflictDetails;
    
    public BookingConflictException(String message, ConflictCheckResult conflictDetails) {
        super(message);
        this.conflictDetails = conflictDetails;
    }
    
    public BookingConflictException(String message, ConflictCheckResult conflictDetails, Throwable cause) {
        super(message, cause);
        this.conflictDetails = conflictDetails;
    }
    
    public ConflictCheckResult getConflictDetails() {
        return conflictDetails;
    }
}