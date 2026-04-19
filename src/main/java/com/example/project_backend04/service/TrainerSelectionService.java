package com.example.project_backend04.service;

import com.example.project_backend04.dto.response.Trainer.TrainerMatchingResult;
import com.example.project_backend04.entity.GymService;
import com.example.project_backend04.entity.ServiceRegistration;
import com.example.project_backend04.entity.TrainerBooking;
import com.example.project_backend04.entity.TrainerSpecialty;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.enums.BookingStatus;
import com.example.project_backend04.repository.GymServiceRepository;
import com.example.project_backend04.repository.ServiceRegistrationRepository;
import com.example.project_backend04.repository.TrainerBookingRepository;
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
    private final TrainerSalaryService trainerSalaryService;
    private final TrainerBookingRepository trainerBookingRepository;

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

        // Check if this trainer has previously rejected a booking for this registration
        List<TrainerBooking> rejectedBookings = trainerBookingRepository
            .findByServiceRegistration_Id(registrationId)
            .stream()
            .filter(b -> b.getStatus() == BookingStatus.REJECTED && 
                        b.getTrainer() != null && 
                        b.getTrainer().getId().equals(trainerId))
            .collect(Collectors.toList());
        
        if (!rejectedBookings.isEmpty()) {
            throw new IllegalArgumentException(
                "Cannot assign this trainer - they have previously rejected a booking for this service registration. " +
                "Rejection reason: " + rejectedBookings.get(0).getRejectionReason()
            );
        }

        TrainerMatchingResult matchResult = matchTrainerToService(trainerId, registration.getGymService().getId());
        if (!matchResult.isMatch()) {
            throw new IllegalArgumentException("Trainer does not match service requirements: " +
                matchResult.getMismatchReason());
        }

        registration.setTrainer(trainer);
        registration.setTrainerSelectedAt(LocalDateTime.now());
        registration.setTrainerSelectionNotes(notes);
        ServiceRegistration savedRegistration = serviceRegistrationRepository.save(registration);

        log.info("Assigned trainer {} to registration {}", trainerId, registrationId);
        
        // Add salary if payment is already SUCCESS
        if (savedRegistration.getPaymentOrder() != null && 
            savedRegistration.getPaymentOrder().getStatus() == com.example.project_backend04.enums.PaymentStatus.SUCCESS) {
            try {
                Long serviceId = savedRegistration.getGymService().getId();
                Long paymentAmount = savedRegistration.getPaymentOrder().getAmount();
                
                log.info("Payment already SUCCESS, adding salary to trainer {} for service {} (paymentAmount={})", 
                    trainerId, serviceId, paymentAmount);
                
                trainerSalaryService.addSalaryToTrainer(trainerId, serviceId, paymentAmount);
                
                log.info("Salary added successfully to trainer {} after assignment", trainerId);
            } catch (Exception e) {
                log.error("Failed to add salary to trainer {} after assignment - continuing", trainerId, e);
            }
        }
        
        // If payment is already SUCCESS and no TrainerBooking exists yet, create a placeholder
        // so the trainer can see the request in their pending-requests tab
        if (savedRegistration.getPaymentOrder() != null &&
            savedRegistration.getPaymentOrder().getStatus() == com.example.project_backend04.enums.PaymentStatus.SUCCESS) {
            
            boolean hasExistingBooking = !trainerBookingRepository
                .findByServiceRegistration_Id(registrationId).isEmpty();
            
            if (!hasExistingBooking) {
                createPendingBookingForServiceRegistration(savedRegistration);
            }
        }
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

    /**
     * Creates a placeholder TrainerBooking so the trainer can see the service request
     * in their pending-requests tab. Used when trainer is assigned after payment is confirmed.
     */
    private void createPendingBookingForServiceRegistration(ServiceRegistration registration) {
        java.time.LocalDate placeholderDate = java.time.LocalDate.now().plusDays(1);
        java.time.LocalTime placeholderStart = java.time.LocalTime.of(9, 0);
        java.time.LocalTime placeholderEnd   = java.time.LocalTime.of(10, 0);

        TrainerBooking booking = TrainerBooking.builder()
                .user(registration.getUser())
                .trainer(registration.getTrainer())
                .serviceRegistration(registration)
                .paymentOrder(registration.getPaymentOrder())
                .bookingDate(placeholderDate)
                .startTime(placeholderStart)
                .endTime(placeholderEnd)
                .notes("In-store booking request – confirm or reschedule")
                .sessionType("SERVICE_REGISTRATION")
                .status(BookingStatus.PENDING)
                .isAssignedByAdmin(true)
                .build();

        TrainerBooking saved = trainerBookingRepository.save(booking);
        log.info("Created placeholder TrainerBooking {} for service registration {} (trainer {})",
                saved.getBookingId(), registration.getId(), registration.getTrainer().getId());
    }
}
