package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.Service.ServiceRegistrationWithTrainerRequest;
import com.example.project_backend04.dto.response.Service.ServiceRegistrationWithTrainerResponse;
import com.example.project_backend04.dto.response.Trainer.TrainerForBookingResponse;
import com.example.project_backend04.entity.GymService;
import com.example.project_backend04.entity.ServiceRegistration;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.enums.RegistrationStatus;
import com.example.project_backend04.repository.GymServiceRepository;
import com.example.project_backend04.repository.ServiceRegistrationRepository;
import com.example.project_backend04.repository.UserRepository;
import com.example.project_backend04.service.IService.ITrainerSelectionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedServiceRegistrationService {
    
    private final ServiceRegistrationRepository serviceRegistrationRepository;
    private final UserRepository userRepository;
    private final GymServiceRepository gymServiceRepository;
    private final ITrainerSelectionService trainerSelectionService;
    private final NotificationService notificationService;
    private final TrainerForBookingService trainerForBookingService;
    
    @Transactional
    public ServiceRegistrationWithTrainerResponse registerServiceWithTrainer(
            ServiceRegistrationWithTrainerRequest request) {
        log.info("Registering service with trainer for user {} and service {}", 
                request.getUserId(), request.getServiceId());
        
        // Validate request
        validateServiceRegistrationRequest(request);
        
        // Get user and service
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        GymService gymService = gymServiceRepository.findById(request.getServiceId())
            .orElseThrow(() -> new EntityNotFoundException("Service not found"));
        
        // Check if user already has active registration for this service
        boolean hasActiveRegistration = serviceRegistrationRepository
            .existsByUserAndGymServiceAndStatus(user, gymService, RegistrationStatus.ACTIVE);
        
        if (hasActiveRegistration) {
            throw new IllegalStateException("User already has an active registration for this service");
        }
        
        // Create service registration
        ServiceRegistration registration = createServiceRegistration(user, gymService, request);
        
        // Assign trainer if specified
        if (request.getTrainerId() != null) {
            assignTrainerToRegistration(registration, request.getTrainerId(), 
                                      request.getTrainerSelectionNotes());
        }
        
        // Save registration
        ServiceRegistration saved = serviceRegistrationRepository.save(registration);
        
        // Send notifications
        notificationService.notifyServiceRegistrationCreated(saved);
        if (saved.hasTrainer()) {
            notificationService.notifyTrainerAssigned(saved);
        }
        
        log.info("Successfully registered service with trainer for registration {}", saved.getId());
        return mapToResponseWithTrainer(saved);
    }
    
    @Transactional(readOnly = true)
    public List<TrainerForBookingResponse> getAvailableTrainers(Long serviceId) {
        if (!gymServiceRepository.existsById(serviceId)) {
            throw new EntityNotFoundException("Service not found with id: " + serviceId);
        }
        return trainerForBookingService.getTrainersByServiceId(serviceId);
    }
    
    @Transactional
    public ServiceRegistrationWithTrainerResponse assignTrainer(Long registrationId, Long trainerId, String notes) {
        log.info("Assigning trainer {} to registration {}", trainerId, registrationId);
        
        ServiceRegistration registration = serviceRegistrationRepository.findById(registrationId)
            .orElseThrow(() -> new EntityNotFoundException("Service registration not found"));
        
        if (registration.getStatus() != RegistrationStatus.ACTIVE) {
            throw new IllegalStateException("Can only assign trainer to active registrations");
        }
        
        // Use trainer selection service to assign trainer
        trainerSelectionService.assignTrainerToServiceRegistration(registrationId, trainerId, notes);
        
        // Reload registration with trainer info
        ServiceRegistration updated = serviceRegistrationRepository
            .findByIdWithFullDetails(registrationId)
            .orElseThrow(() -> new EntityNotFoundException("Registration not found after update"));
        
        // Send notification
        notificationService.notifyTrainerAssigned(updated);
        
        log.info("Successfully assigned trainer {} to registration {}", trainerId, registrationId);
        return mapToResponseWithTrainer(updated);
    }
    
    @Transactional(readOnly = true)
    public ServiceRegistrationWithTrainerResponse getRegistrationFullDetails(Long registrationId) {
        log.debug("Getting full details for registration {}", registrationId);
        
        ServiceRegistration registration = serviceRegistrationRepository
            .findByIdWithFullDetails(registrationId)
            .orElseThrow(() -> new EntityNotFoundException("Service registration not found"));
        
        return mapToResponseWithTrainer(registration);
    }
    
    @Transactional(readOnly = true)
    public List<ServiceRegistrationWithTrainerResponse> getUserRegistrationsWithTrainers(Long userId) {
        log.debug("Getting registrations with trainers for user {}", userId);
        
        List<ServiceRegistration> registrations = serviceRegistrationRepository
            .findByUserIdAndStatusWithTrainerAndService(userId, RegistrationStatus.ACTIVE);
        
        return registrations.stream()
            .map(this::mapToResponseWithTrainer)
            .toList();
    }
    
    @Transactional(readOnly = true)
    public List<ServiceRegistrationWithTrainerResponse> getTrainerRegistrations(Long trainerId) {
        log.debug("Getting registrations for trainer {}", trainerId);
        
        List<ServiceRegistration> registrations = serviceRegistrationRepository
            .findByTrainerIdAndStatus(trainerId, RegistrationStatus.ACTIVE);
        
        return registrations.stream()
            .map(this::mapToResponseWithTrainer)
            .toList();
    }
    
    @Transactional
    public ServiceRegistrationWithTrainerResponse updateTrainerNotes(Long registrationId, String notes) {
        log.info("Updating trainer notes for registration {}", registrationId);
        
        ServiceRegistration registration = serviceRegistrationRepository.findById(registrationId)
            .orElseThrow(() -> new EntityNotFoundException("Service registration not found"));
        
        if (!registration.hasTrainer()) {
            throw new IllegalStateException("Registration does not have an assigned trainer");
        }
        
        registration.setTrainerSelectionNotes(notes);
        ServiceRegistration saved = serviceRegistrationRepository.save(registration);
        
        return mapToResponseWithTrainer(saved);
    }
    
    @Transactional
    public void removeTrainerFromRegistration(Long registrationId, String reason) {
        log.info("Removing trainer from registration {} - reason: {}", registrationId, reason);
        
        ServiceRegistration registration = serviceRegistrationRepository.findById(registrationId)
            .orElseThrow(() -> new EntityNotFoundException("Service registration not found"));
        
        if (!registration.hasTrainer()) {
            throw new IllegalStateException("Registration does not have an assigned trainer");
        }
        
        registration.setTrainer(null);
        registration.setTrainerSelectedAt(null);
        registration.setTrainerSelectionNotes("Trainer removed: " + reason);
        
        serviceRegistrationRepository.save(registration);
        
        log.info("Successfully removed trainer from registration {}", registrationId);
    }
    
    private void validateServiceRegistrationRequest(ServiceRegistrationWithTrainerRequest request) {
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        
        if (request.getServiceId() == null) {
            throw new IllegalArgumentException("Service ID is required");
        }
        
        // Validate trainer if specified
        if (request.getTrainerId() != null) {
            User trainer = userRepository.findById(request.getTrainerId())
                .orElseThrow(() -> new EntityNotFoundException("Trainer not found"));
            
            if (!trainer.isTrainer()) {
                throw new IllegalArgumentException("Specified user is not a trainer");
            }
            
            if (!trainer.getIsActive()) {
                throw new IllegalArgumentException("Trainer is not active");
            }
        }
    }
    
    private ServiceRegistration createServiceRegistration(User user, GymService gymService, 
                                                        ServiceRegistrationWithTrainerRequest request) {
        ServiceRegistration registration = new ServiceRegistration();
        registration.setUser(user);
        registration.setGymService(gymService);
        registration.setStatus(RegistrationStatus.ACTIVE);
        registration.setNotes(request.getNotes());
        
        return registration;
    }
    
    private void assignTrainerToRegistration(ServiceRegistration registration, Long trainerId, String notes) {
        User trainer = userRepository.findById(trainerId)
            .orElseThrow(() -> new EntityNotFoundException("Trainer not found"));
        
        // Validate trainer matches service requirements
        var matchResult = trainerSelectionService.matchTrainerToService(
            trainerId, registration.getGymService().getId());
        
        if (!matchResult.isMatch()) {
            throw new IllegalArgumentException("Trainer does not match service requirements: " + 
                matchResult.getMismatchReason());
        }
        
        registration.setTrainer(trainer);
        registration.setTrainerSelectedAt(LocalDateTime.now());
        registration.setTrainerSelectionNotes(notes);
    }
    
    private ServiceRegistrationWithTrainerResponse mapToResponseWithTrainer(ServiceRegistration registration) {
        // This would use a proper mapper in real implementation
        // For now, create a basic response
        return ServiceRegistrationWithTrainerResponse.builder()
            .id(registration.getId())
            .userId(registration.getUser().getId())
            .serviceId(registration.getGymService().getId())
            .serviceName(registration.getGymService().getName())
            .trainerId(registration.getTrainer() != null ? registration.getTrainer().getId() : null)
            .trainerName(registration.getTrainer() != null ? registration.getTrainer().getFullName() : null)
            .status(registration.getStatus())
            .registrationDate(registration.getRegistrationDate())
            .trainerSelectedAt(registration.getTrainerSelectedAt())
            .trainerSelectionNotes(registration.getTrainerSelectionNotes())
            .notes(registration.getNotes())
            .hasTrainer(registration.hasTrainer())
            .canBookTrainer(registration.canBookTrainer())
            .totalBookings(registration.getTotalBookingsCount())
            .build();
    }
}