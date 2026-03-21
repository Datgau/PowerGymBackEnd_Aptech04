package com.example.project_backend04.exception;

import java.util.List;

public class TrainerSpecialtyMismatchException extends RuntimeException {
    
    private final Long trainerId;
    private final Long serviceId;
    private final List<String> requiredSpecialties;
    private final List<String> trainerSpecialties;
    
    public TrainerSpecialtyMismatchException(String message, Long trainerId, Long serviceId,
                                           List<String> requiredSpecialties, 
                                           List<String> trainerSpecialties) {
        super(message);
        this.trainerId = trainerId;
        this.serviceId = serviceId;
        this.requiredSpecialties = requiredSpecialties;
        this.trainerSpecialties = trainerSpecialties;
    }
    
    public Long getTrainerId() {
        return trainerId;
    }
    
    public Long getServiceId() {
        return serviceId;
    }
    
    public List<String> getRequiredSpecialties() {
        return requiredSpecialties;
    }
    
    public List<String> getTrainerSpecialties() {
        return trainerSpecialties;
    }
}