package com.example.project_backend04.service.IService;

import com.example.project_backend04.dto.response.Trainer.TrainerMatchingResult;

import java.time.LocalDate;

public interface ITrainerSelectionService {

    /**
     * Check if trainer matches service requirements
     */
    TrainerMatchingResult matchTrainerToService(Long trainerId, Long serviceId);

    /**
     * Assign trainer to service registration
     */
    void assignTrainerToServiceRegistration(Long registrationId, Long trainerId, String notes);

    /**
     * Check trainer availability for specific date
     */
    boolean isTrainerAvailable(Long trainerId, LocalDate date);
}