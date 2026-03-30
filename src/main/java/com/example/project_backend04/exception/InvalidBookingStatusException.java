package com.example.project_backend04.exception;


import com.example.project_backend04.enums.BookingStatus;

public class InvalidBookingStatusException extends RuntimeException {
    
    private final Long bookingId;
    private final BookingStatus currentStatus;
    private final BookingStatus requiredStatus;
    
    public InvalidBookingStatusException(String message, Long bookingId, 
                                       BookingStatus currentStatus,
                                       BookingStatus requiredStatus) {
        super(message);
        this.bookingId = bookingId;
        this.currentStatus = currentStatus;
        this.requiredStatus = requiredStatus;
    }
    
    public InvalidBookingStatusException(String message, Long bookingId, 
                                       BookingStatus currentStatus) {
        super(message);
        this.bookingId = bookingId;
        this.currentStatus = currentStatus;
        this.requiredStatus = null;
    }
    
    public Long getBookingId() {
        return bookingId;
    }
    
    public BookingStatus getCurrentStatus() {
        return currentStatus;
    }
    
    public BookingStatus getRequiredStatus() {
        return requiredStatus;
    }
}