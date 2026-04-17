package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.Service.ServiceRegistrationWithTrainerRequest;
import com.example.project_backend04.dto.response.Service.ServiceRegistrationWithTrainerResponse;
import com.example.project_backend04.dto.response.Trainer.TrainerForBookingResponse;
import com.example.project_backend04.entity.GymService;
import com.example.project_backend04.entity.PaymentOrder;
import com.example.project_backend04.entity.ServiceRegistration;
import com.example.project_backend04.entity.TrainerBooking;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.enums.PaymentStatus;
import com.example.project_backend04.enums.RegistrationStatus;
import com.example.project_backend04.enums.RegistrationType;
import com.example.project_backend04.enums.BookingStatus;
import com.example.project_backend04.repository.GymServiceRepository;
import com.example.project_backend04.repository.PaymentOrderRepository;
import com.example.project_backend04.repository.ServiceRegistrationRepository;
import com.example.project_backend04.repository.TrainerBookingRepository;
import com.example.project_backend04.repository.UserRepository;
import com.example.project_backend04.service.IService.ITrainerSelectionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
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
    private final TrainerBookingService trainerBookingService;
    private final PaymentOrderRepository paymentOrderRepository;
    private final TrainerSalaryService trainerSalaryService;
    private final TrainerBookingRepository trainerBookingRepository;
    
    @Transactional
    public ServiceRegistrationWithTrainerResponse registerServiceWithTrainer(
            ServiceRegistrationWithTrainerRequest request) {
        log.info("Registering service with trainer for user {} and service {}", 
                request.getUserId(), request.getServiceId());
        validateServiceRegistrationRequest(request);
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        GymService gymService = gymServiceRepository.findById(request.getServiceId())
            .orElseThrow(() -> new EntityNotFoundException("Service not found"));
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
        return trainerBookingService.getTrainersByServiceId(serviceId);
    }
    
    @Transactional
    public ServiceRegistrationWithTrainerResponse assignTrainer(Long registrationId, Long trainerId, String notes) {
        log.info("Assigning trainer {} to registration {}", trainerId, registrationId);
        
        ServiceRegistration registration = serviceRegistrationRepository.findById(registrationId)
            .orElseThrow(() -> new EntityNotFoundException("Service registration not found"));
        
        // Allow assignment for ACTIVE registrations (online, already paid)
        // and PENDING COUNTER registrations (will pay at counter after trainer is assigned)
        boolean isActive = registration.getStatus() == RegistrationStatus.ACTIVE;
        boolean isPendingCounter = registration.getStatus() == RegistrationStatus.PENDING
                && registration.getRegistrationType() == RegistrationType.COUNTER;

        if (!isActive && !isPendingCounter) {
            throw new IllegalStateException("Can only assign trainer to active registrations or pending counter registrations");
        }
        
        // Use trainer selection service to assign trainer (this will also add salary if payment is SUCCESS)
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
        var user = registration.getUser();
        var svc  = registration.getGymService();
        var trainer = registration.getTrainer();
        return ServiceRegistrationWithTrainerResponse.builder()
            .id(registration.getId())
            .userId(user != null ? user.getId() : null)
            .serviceId(svc != null ? svc.getId() : null)
            .serviceName(svc != null && svc.getName() != null ? svc.getName() : "")
            .userName(user != null && user.getFullName() != null ? user.getFullName() : "")
            .userEmail(user != null && user.getEmail() != null ? user.getEmail() : "")
            .trainerAvatar(user != null ? user.getAvatar() : null)
            .trainerId(trainer != null ? trainer.getId() : null)
            .trainerName(trainer != null ? trainer.getFullName() : null)
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

    @Transactional
    public void confirmCounterPayment(Long registrationId, Long amount,
                                      java.time.LocalDate bookingDate,
                                      java.time.LocalTime startTime,
                                      java.time.LocalTime endTime) {
        log.info("Confirming counter payment for registration {} with amount {}", registrationId, amount);
        
        ServiceRegistration registration = serviceRegistrationRepository.findById(registrationId)
            .orElseThrow(() -> new EntityNotFoundException("Service registration not found"));
        
        if (registration.getRegistrationType() != RegistrationType.COUNTER) {
            throw new IllegalStateException("Can only confirm payment for counter registrations");
        }
        LocalDateTime now = LocalDateTime.now();
        registration.setRegistrationDate(now);
        registration.setStatus(RegistrationStatus.ACTIVE); // Activate after payment confirmed at counter
        
        if (registration.getGymService() != null && registration.getGymService().getDuration() != null) {
            registration.setExpirationDate(now.plusDays(registration.getGymService().getDuration()));
        }
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setId("COUNTER_" + registrationId + "_" + System.currentTimeMillis());
        paymentOrder.setUser(registration.getUser());
        paymentOrder.setAmount(amount);
        paymentOrder.setStatus(PaymentStatus.SUCCESS);
        paymentOrder.setItemType("SERVICE");
        paymentOrder.setItemId(registration.getGymService().getId().toString());
        paymentOrder.setItemName(registration.getGymService().getName());
        paymentOrder.setContent("Counter payment for " + registration.getGymService().getName());
        paymentOrder.setPaymentMethod("COUNTER");
        paymentOrder.setCreatedAt(now);
        
        PaymentOrder savedPaymentOrder = paymentOrderRepository.save(paymentOrder);
        
        // Link payment order to registration
        registration.setPaymentOrder(savedPaymentOrder);
        ServiceRegistration savedRegistration = serviceRegistrationRepository.save(registration);
        
        // Link payment order to all PENDING TrainerBookings of this registration
        // (bookings created before payment was confirmed will have paymentOrder = null)
        List<TrainerBooking> pendingBookings = trainerBookingRepository
            .findByServiceRegistration_Id(registrationId)
            .stream()
            .filter(b -> b.getStatus() == BookingStatus.PENDING && b.getPaymentOrder() == null)
            .collect(java.util.stream.Collectors.toList());
        
        if (!pendingBookings.isEmpty()) {
            for (TrainerBooking booking : pendingBookings) {
                booking.setPaymentOrder(savedPaymentOrder);
            }
            trainerBookingRepository.saveAll(pendingBookings);
            log.info("Linked PaymentOrder {} to {} pending TrainerBooking(s) for registration {}",
                savedPaymentOrder.getId(), pendingBookings.size(), registrationId);
        } else if (savedRegistration.getTrainer() != null) {
            // No TrainerBooking exists yet — create one so the trainer can see the request
            createPendingBookingForServiceRegistration(savedRegistration, savedPaymentOrder, bookingDate, startTime, endTime);
        }
        
        log.info("Successfully confirmed counter payment for registration {} - Service start date: {}, expiration date: {}", 
            registrationId, registration.getRegistrationDate(), registration.getExpirationDate());
        
        // Add salary if trainer is already assigned
        if (savedRegistration.getTrainer() != null) {
            try {
                Long trainerId = savedRegistration.getTrainer().getId();
                Long serviceId = savedRegistration.getGymService().getId();
                
                log.info("Trainer already assigned, adding salary to trainer {} for service {} (paymentAmount={})", 
                    trainerId, serviceId, amount);
                
                trainerSalaryService.addSalaryToTrainer(trainerId, serviceId, amount);
                
                log.info("Salary added successfully to trainer {} after counter payment confirmation", trainerId);
            } catch (Exception e) {
                log.error("Failed to add salary to trainer after counter payment - continuing", e);
            }
        }
    }

    /**
     * Creates a TrainerBooking with the admin-selected date/time so the trainer can see
     * the request in their pending-requests tab.
     */
    private void createPendingBookingForServiceRegistration(
            ServiceRegistration registration, PaymentOrder paymentOrder,
            java.time.LocalDate bookingDate, java.time.LocalTime startTime, java.time.LocalTime endTime) {

        // Fall back to tomorrow 09:00-10:00 only if no date/time was provided
        java.time.LocalDate date  = bookingDate != null ? bookingDate : java.time.LocalDate.now().plusDays(1);
        java.time.LocalTime start = startTime  != null ? startTime  : java.time.LocalTime.of(9, 0);
        java.time.LocalTime end   = endTime    != null ? endTime    : java.time.LocalTime.of(10, 0);

        TrainerBooking booking = TrainerBooking.builder()
                .user(registration.getUser())
                .trainer(registration.getTrainer())
                .serviceRegistration(registration)
                .paymentOrder(paymentOrder)
                .bookingDate(date)
                .startTime(start)
                .endTime(end)
                .notes("Yêu cầu đặt lịch từ đăng ký dịch vụ tại quầy - vui lòng xác nhận hoặc đổi lịch")
                .sessionType("SERVICE_REGISTRATION")
                .status(BookingStatus.PENDING)
                .isAssignedByAdmin(true)
                .build();

        TrainerBooking saved = trainerBookingRepository.save(booking);
        log.info("Created TrainerBooking {} for service registration {} (trainer {}) on {} {}–{}",
                saved.getBookingId(), registration.getId(), registration.getTrainer().getId(),
                date, start, end);
    }
}
