package com.example.project_backend04.exception;

import com.example.project_backend04.entity.TrainerBooking;

public class InvalidBookingStatusException extends RuntimeException {
    
    private final Long bookingId;
    private final TrainerBooking.BookingStatus currentStatus;
    private final TrainerBooking.BookingStatus requiredStatus;
    
    public InvalidBookingStatusException(String message, Long bookingId, 
                                       TrainerBooking.BookingStatus currentStatus,
                                       TrainerBooking.BookingStatus requiredStatus) {
        super(message);
        this.bookingId = bookingId;
        this.currentStatus = currentStatus;
        this.requiredStatus = requiredStatus;
    }
    
    public InvalidBookingStatusException(String message, Long bookingId, 
                                       TrainerBooking.BookingStatus currentStatus) {
        super(message);
        this.bookingId = bookingId;
        this.currentStatus = currentStatus;
        this.requiredStatus = null;
    }
    
    public Long getBookingId() {
        return bookingId;
    }
    
    public TrainerBooking.BookingStatus getCurrentStatus() {
        return currentStatus;
    }
    
    public TrainerBooking.BookingStatus getRequiredStatus() {
        return requiredStatus;
    }
}