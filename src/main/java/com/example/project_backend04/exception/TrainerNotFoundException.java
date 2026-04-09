package com.example.project_backend04.exception;

/**
 * Exception thrown when a trainer is not found
 */
public class TrainerNotFoundException extends RuntimeException {
    
    private final Long trainerId;
    
    public TrainerNotFoundException(Long trainerId) {
        super("Trainer not found with id: " + trainerId);
        this.trainerId = trainerId;
    }
    
    public Long getTrainerId() {
        return trainerId;
    }
}
