package com.example.project_backend04.service.IService;

import com.example.project_backend04.dto.response.Trainer.TrainerAvailabilityDTO;
import com.example.project_backend04.dto.response.Trainer.TrainerMatchingResult;
import com.example.project_backend04.entity.User;

import java.time.LocalDate;
import java.util.List;

public interface ITrainerSelectionService {
    
    /**
     * Find available trainers for a specific service with optional preferred date
     */
    List<TrainerAvailabilityDTO> findAvailableTrainers(Long serviceId, LocalDate preferredDate);
    
    /**
     * Find trainers by specialty category
     */
    List<User> findTrainersBySpecialty(Long serviceCategoryId);
    
    /**
     * Check if trainer matches service requirements
     */
    TrainerMatchingResult matchTrainerToService(Long trainerId, Long serviceId);
    
    /**
     * Assign trainer to service registration
     */
    void assignTrainerToServiceRegistration(Long registrationId, Long trainerId, String notes);
    
    /**
     * Get trainer details with specialties and statistics
     */
    TrainerAvailabilityDTO getTrainerDetails(Long trainerId, LocalDate date);
    
    /**
     * Find top-rated trainers for a service category
     */
    List<TrainerAvailabilityDTO> findTopTrainersForCategory(Long categoryId, int limit);
    
    /**
     * Check trainer availability for specific date
     */
    boolean isTrainerAvailable(Long trainerId, LocalDate date);
}