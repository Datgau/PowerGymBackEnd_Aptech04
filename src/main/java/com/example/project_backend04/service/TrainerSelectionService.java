package com.example.project_backend04.service;

import com.example.project_backend04.dto.response.Trainer.TrainerMatchingResult;
import com.example.project_backend04.entity.GymService;
import com.example.project_backend04.entity.ServiceRegistration;
import com.example.project_backend04.entity.TrainerSpecialty;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.repository.GymServiceRepository;
import com.example.project_backend04.repository.ServiceRegistrationRepository;
import com.example.project_backend04.repository.TrainerSpecialtyRepository;
import com.example.project_backend04.repository.UserRepository;
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
public class TrainerSelectionService implements ITrainerSelectionService {

    private final TrainerSpecialtyRepository trainerSpecialtyRepository;
    private final ServiceRegistrationRepository serviceRegistrationRepository;
    private final GymServiceRepository gymServiceRepository;
    private final UserRepository userRepository;
    private final IConflictDetectionService conflictDetectionService;

    @Override
    @Transactional(readOnly = true)
    public TrainerMatchingResult matchTrainerToService(Long trainerId, Long serviceId) {
        User trainer = userRepository.findById(trainerId)
            .orElseThrow(() -> new EntityNotFoundException("Trainer not found"));

        GymService service = gymServiceRepository.findById(serviceId)
            .orElseThrow(() -> new EntityNotFoundException("Service not found"));

        boolean hasSpecialty = trainerSpecialtyRepository.hasTrainerSpecialtyForCategory(
            trainerId, service.getCategory().getId());

        if (!hasSpecialty) {
            return TrainerMatchingResult.noMatch(
                "Trainer does not have required specialty for this service",
                List.of(service.getCategory().getDisplayName())
            );
        }

        List<TrainerSpecialty> specialties = trainerSpecialtyRepository
            .findActiveSpecialtiesByTrainer(trainerId);

        List<String> matchingSpecialties = specialties.stream()
            .filter(ts -> ts.getSpecialty().getId().equals(service.getCategory().getId()))
            .map(ts -> ts.getSpecialty().getDisplayName())
            .collect(Collectors.toList());

        return TrainerMatchingResult.match(matchingSpecialties, calculateMatchScore(specialties, service.getCategory().getId()));
    }

    @Override
    @Transactional
    public void assignTrainerToServiceRegistration(Long registrationId, Long trainerId, String notes) {
        ServiceRegistration registration = serviceRegistrationRepository.findById(registrationId)
            .orElseThrow(() -> new EntityNotFoundException("Service registration not found"));

        User trainer = userRepository.findById(trainerId)
            .orElseThrow(() -> new EntityNotFoundException("Trainer not found"));

        TrainerMatchingResult matchResult = matchTrainerToService(trainerId, registration.getGymService().getId());
        if (!matchResult.isMatch()) {
            throw new IllegalArgumentException("Trainer does not match service requirements: " +
                matchResult.getMismatchReason());
        }

        registration.setTrainer(trainer);
        registration.setTrainerSelectedAt(LocalDateTime.now());
        registration.setTrainerSelectionNotes(notes);
        serviceRegistrationRepository.save(registration);

        log.info("Assigned trainer {} to registration {}", trainerId, registrationId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTrainerAvailable(Long trainerId, LocalDate date) {
        return !conflictDetectionService.getAvailableSlots(trainerId, date).isEmpty();
    }

    private double calculateMatchScore(List<TrainerSpecialty> specialties, Long categoryId) {
        return specialties.stream()
            .filter(ts -> ts.getSpecialty().getId().equals(categoryId))
            .findFirst()
            .map(ts -> {
                double score = 0.5;
                if (ts.getExperienceYears() != null) score += Math.min(0.3, ts.getExperienceYears() * 0.05);
                if (ts.getLevel() != null) {
                    score += switch (ts.getLevel().toUpperCase()) {
                        case "EXPERT" -> 0.2;
                        case "ADVANCED" -> 0.15;
                        case "INTERMEDIATE" -> 0.1;
                        default -> 0.05;
                    };
                }
                return Math.min(1.0, score);
            })
            .orElse(0.0);
    }
}
