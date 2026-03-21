package com.example.project_backend04.service;

import com.example.project_backend04.dto.response.ServiceCategory.ServiceCategoryResponse;
import com.example.project_backend04.dto.response.Trainer.TrainerAvailabilityDTO;
import com.example.project_backend04.dto.response.Trainer.TrainerMatchingResult;
import com.example.project_backend04.dto.response.Trainer.TrainerSpecialtyResponse;
import com.example.project_backend04.dto.response.TrainerBooking.TimeSlot;
import com.example.project_backend04.dto.response.User.UserResponse;
import com.example.project_backend04.entity.*;
import com.example.project_backend04.mapper.UserMapper;
import com.example.project_backend04.repository.*;
import com.example.project_backend04.service.IService.IConflictDetectionService;
import com.example.project_backend04.service.IService.ITrainerSelectionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TrainerSelectionService implements ITrainerSelectionService {
    
    private final TrainerSpecialtyRepository trainerSpecialtyRepository;
    private final ServiceRegistrationRepository serviceRegistrationRepository;
    private final GymServiceRepository gymServiceRepository;
    private final TrainerBookingRepository trainerBookingRepository;
    private final UserRepository userRepository;
    private final IConflictDetectionService conflictDetectionService;
    private final UserMapper userMapper;
    
    @Override
    public List<TrainerAvailabilityDTO> findAvailableTrainers(Long serviceId, LocalDate preferredDate) {
        log.info("Finding available trainers for service {} on date {}", serviceId, preferredDate);
        
        // Get service and its category
        GymService service = gymServiceRepository.findById(serviceId)
            .orElseThrow(() -> new EntityNotFoundException("Service not found with id: " + serviceId));
        
        // Find trainers with matching specialties
        List<User> trainers = trainerSpecialtyRepository.findTrainersBySpecialtyCategory(
            service.getCategory().getId());
        
        // Build availability DTOs for each trainer
        return trainers.stream()
            .map(trainer -> buildTrainerAvailabilityDTO(trainer, preferredDate))
            .filter(dto -> dto.isAvailable() && dto.hasAvailableSlots())
            .sorted((a, b) -> {
                // Sort by rating (desc), then by experience (desc)
                int ratingCompare = Double.compare(
                    b.getAverageRating() != null ? b.getAverageRating() : 0.0,
                    a.getAverageRating() != null ? a.getAverageRating() : 0.0
                );
                if (ratingCompare != 0) return ratingCompare;
                
                return Integer.compare(
                    b.getTotalExperience() != null ? b.getTotalExperience() : 0,
                    a.getTotalExperience() != null ? a.getTotalExperience() : 0
                );
            })
            .collect(Collectors.toList());
    }
    
    @Override
    public List<User> findTrainersBySpecialty(Long serviceCategoryId) {
        log.info("Finding trainers by specialty category {}", serviceCategoryId);
        return trainerSpecialtyRepository.findTrainersBySpecialtyCategory(serviceCategoryId);
    }
    
    @Override
    public TrainerMatchingResult matchTrainerToService(Long trainerId, Long serviceId) {
        log.info("Matching trainer {} to service {}", trainerId, serviceId);
        
        User trainer = userRepository.findById(trainerId)
            .orElseThrow(() -> new EntityNotFoundException("Trainer not found"));
        
        GymService service = gymServiceRepository.findById(serviceId)
            .orElseThrow(() -> new EntityNotFoundException("Service not found"));
        
        // Check if trainer has required specialty
        boolean hasSpecialty = trainerSpecialtyRepository.hasTrainerSpecialtyForCategory(
            trainerId, service.getCategory().getId());
        
        if (!hasSpecialty) {
            return TrainerMatchingResult.noMatch(
                "Trainer does not have required specialty for this service",
                List.of(service.getCategory().getDisplayName())
            );
        }
        
        // Get trainer's specialties for this category
        List<TrainerSpecialty> specialties = trainerSpecialtyRepository
            .findActiveSpecialtiesByTrainer(trainerId);
        
        List<String> matchingSpecialties = specialties.stream()
            .filter(ts -> ts.getSpecialty().getId().equals(service.getCategory().getId()))
            .map(ts -> ts.getSpecialty().getDisplayName())
            .collect(Collectors.toList());
        
        // Calculate match score based on experience and level
        double matchScore = calculateMatchScore(specialties, service.getCategory().getId());
        
        return TrainerMatchingResult.match(matchingSpecialties, matchScore);
    }
    
    @Override
    public void assignTrainerToServiceRegistration(Long registrationId, Long trainerId, String notes) {
        log.info("Assigning trainer {} to service registration {}", trainerId, registrationId);
        
        ServiceRegistration registration = serviceRegistrationRepository.findById(registrationId)
            .orElseThrow(() -> new EntityNotFoundException("Service registration not found"));
        
        User trainer = userRepository.findById(trainerId)
            .orElseThrow(() -> new EntityNotFoundException("Trainer not found"));
        
        // Validate trainer has required specialty
        TrainerMatchingResult matchResult = matchTrainerToService(trainerId, registration.getGymService().getId());
        if (!matchResult.isMatch()) {
            throw new IllegalArgumentException("Trainer does not match service requirements: " + 
                matchResult.getMismatchReason());
        }
        
        // Assign trainer
        registration.setTrainer(trainer);
        registration.setTrainerSelectedAt(LocalDateTime.now());
        registration.setTrainerSelectionNotes(notes);
        
        serviceRegistrationRepository.save(registration);
        
        log.info("Successfully assigned trainer {} to service registration {}", trainerId, registrationId);
    }
    
    @Override
    public TrainerAvailabilityDTO getTrainerDetails(Long trainerId, LocalDate date) {
        User trainer = userRepository.findById(trainerId)
            .orElseThrow(() -> new EntityNotFoundException("Trainer not found"));
        
        return buildTrainerAvailabilityDTO(trainer, date);
    }
    
    @Override
    public List<TrainerAvailabilityDTO> findTopTrainersForCategory(Long categoryId, int limit) {
        List<TrainerSpecialty> topSpecialties = trainerSpecialtyRepository
            .findTopTrainersByCategory(categoryId);
        
        return topSpecialties.stream()
            .limit(limit)
            .map(ts -> buildTrainerAvailabilityDTO(ts.getUser(), LocalDate.now()))
            .collect(Collectors.toList());
    }
    
    @Override
    public boolean isTrainerAvailable(Long trainerId, LocalDate date) {
        List<TimeSlot> availableSlots = conflictDetectionService.getAvailableSlots(trainerId, date);
        return !availableSlots.isEmpty();
    }
    
    private TrainerAvailabilityDTO buildTrainerAvailabilityDTO(User trainer, LocalDate date) {
        // Get trainer specialties
        List<TrainerSpecialty> specialties = trainerSpecialtyRepository
            .findActiveSpecialtiesByTrainer(trainer.getId());
        
        List<TrainerSpecialtyResponse> specialtyResponses = specialties.stream()
            .map(this::mapToSpecialtyResponse)
            .collect(Collectors.toList());
        
        // Get availability for the date
        List<TimeSlot> availableSlots = date != null ? 
            conflictDetectionService.getAvailableSlots(trainer.getId(), date) : 
            List.of();
        
        // Get statistics
        Double averageRating = trainerBookingRepository.findAverageRatingByTrainer(trainer.getId());
        Long completedSessions = trainerBookingRepository.countByTrainerAndDateRangeAndStatus(
            trainer.getId(), 
            LocalDate.now().minusYears(1), 
            LocalDate.now(),
            TrainerBooking.BookingStatus.COMPLETED
        );
        
        UserResponse userResponse = userMapper.toResponse(trainer);
        
        return TrainerAvailabilityDTO.builder()
            .trainer(userResponse)
            .specialties(specialtyResponses)
            .availableSlots(availableSlots)
            .totalExperience(trainer.getTotalExperienceYears())
            .averageRating(averageRating)
            .completedSessions(completedSessions != null ? completedSessions.intValue() : 0)
            .bio(trainer.getBio())
            .education(trainer.getEducation())
            .isAvailable(!availableSlots.isEmpty())
            .unavailabilityReason(availableSlots.isEmpty() ? "No available slots" : null)
            .build();
    }
    
    private TrainerSpecialtyResponse mapToSpecialtyResponse(TrainerSpecialty specialty) {
        ServiceCategoryResponse categoryResponse = new ServiceCategoryResponse();
        categoryResponse.setId(specialty.getSpecialty().getId());
        categoryResponse.setName(specialty.getSpecialty().getName());
        categoryResponse.setDisplayName(specialty.getSpecialty().getDisplayName());
        categoryResponse.setDescription(specialty.getSpecialty().getDescription());
        categoryResponse.setIcon(specialty.getSpecialty().getIcon());
        categoryResponse.setColor(specialty.getSpecialty().getColor());
        categoryResponse.setIsActive(specialty.getSpecialty().getIsActive());
        
        return TrainerSpecialtyResponse.builder()
            .id(specialty.getId())
            .specialty(categoryResponse)
            .description(specialty.getDescription())
            .experienceYears(specialty.getExperienceYears())
            .level(specialty.getLevel())
            .certifications(specialty.getCertifications())
            .isActive(specialty.getIsActive())
            .createdAt(specialty.getCreatedAt())
            .build();
    }
    
    private double calculateMatchScore(List<TrainerSpecialty> specialties, Long categoryId) {
        TrainerSpecialty matchingSpecialty = specialties.stream()
            .filter(ts -> ts.getSpecialty().getId().equals(categoryId))
            .findFirst()
            .orElse(null);
        
        if (matchingSpecialty == null) return 0.0;
        
        double score = 0.5; // Base score for having the specialty
        
        // Add score for experience
        if (matchingSpecialty.getExperienceYears() != null) {
            score += Math.min(0.3, matchingSpecialty.getExperienceYears() * 0.05);
        }
        
        // Add score for level
        if (matchingSpecialty.getLevel() != null) {
            switch (matchingSpecialty.getLevel().toUpperCase()) {
                case "EXPERT": score += 0.2; break;
                case "ADVANCED": score += 0.15; break;
                case "INTERMEDIATE": score += 0.1; break;
                case "BEGINNER": score += 0.05; break;
            }
        }
        
        return Math.min(1.0, score);
    }
}
