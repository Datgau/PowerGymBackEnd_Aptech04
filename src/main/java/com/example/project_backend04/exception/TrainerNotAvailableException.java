package com.example.project_backend04.exception;

import java.time.LocalDate;
import java.time.LocalTime;

public class TrainerNotAvailableException extends RuntimeException {
    
    private final Long trainerId;
    private final LocalDate requestedDate;
    private final LocalTime requestedStartTime;
    private final LocalTime requestedEndTime;
    
    public TrainerNotAvailableException(String message, Long trainerId, LocalDate requestedDate, 
                                      LocalTime requestedStartTime, LocalTime requestedEndTime) {
        super(message);
        this.trainerId = trainerId;
        this.requestedDate = requestedDate;
        this.requestedStartTime = requestedStartTime;
        this.requestedEndTime = requestedEndTime;
    }
    
    public TrainerNotAvailableException(String message, Long trainerId) {
        super(message);
        this.trainerId = trainerId;
        this.requestedDate = null;
        this.requestedStartTime = null;
        this.requestedEndTime = null;
    }
    
    public Long getTrainerId() {
        return trainerId;
    }
    
    public LocalDate getRequestedDate() {
        return requestedDate;
    }
    
    public LocalTime getRequestedStartTime() {
        return requestedStartTime;
    }
    
    public LocalTime getRequestedEndTime() {
        return requestedEndTime;
    }
}