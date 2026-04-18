package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.Service.ServiceRegistrationRequest;
import com.example.project_backend04.dto.response.Service.GymServiceResponse;
import com.example.project_backend04.dto.response.Service.ServiceRegistrationResponse;
import com.example.project_backend04.dto.response.Service.ServiceRegistrationWithTrainerSelectionResponse;
import com.example.project_backend04.dto.response.Trainer.AvailableTrainerResponse;
import com.example.project_backend04.dto.response.Trainer.TrainerAvailabilityDTO;
import com.example.project_backend04.dto.response.Trainer.TrainerForBookingResponse;
import com.example.project_backend04.dto.response.User.UserResponse;
import com.example.project_backend04.entity.GymService;
import com.example.project_backend04.entity.PaymentOrder;
import com.example.project_backend04.entity.ServiceCategory;
import com.example.project_backend04.entity.ServiceRegistration;
import com.example.project_backend04.entity.TrainerBooking;
import com.example.project_backend04.entity.TrainerSpecialty;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.enums.BookingStatus;
import com.example.project_backend04.enums.PaymentStatus;
import com.example.project_backend04.enums.RegistrationStatus;
import com.example.project_backend04.enums.RegistrationType;
import com.example.project_backend04.event.EntityChangedEvent;
import com.example.project_backend04.mapper.UserMapper;
import com.example.project_backend04.repository.GymServiceRepository;
import com.example.project_backend04.repository.PaymentOrderRepository;
import com.example.project_backend04.repository.ServiceRegistrationRepository;
import com.example.project_backend04.repository.TrainerBookingRepository;
import com.example.project_backend04.repository.TrainerSpecialtyRepository;
import com.example.project_backend04.repository.UserRepository;
import com.example.project_backend04.service.EnhancedServiceRegistrationService;
import com.example.project_backend04.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import com.example.project_backend04.dto.request.Service.ServiceRegistrationFilterRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceRegistrationService {

    private final ServiceRegistrationRepository registrationRepository;
    private final GymServiceRepository gymServiceRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final GymServiceService gymServiceService;
    private final ApplicationEventPublisher eventPublisher;
    private final TrainerBookingRepository trainerBookingRepository;
    private final EnhancedServiceRegistrationService enhancedServiceRegistrationService;
    private final PaymentOrderRepository paymentOrderRepository;
    private final TrainerSpecialtyRepository trainerSpecialtyRepository;
    private final EmailService emailService;
    private final com.example.project_backend04.mapper.ServiceRegistrationMapper serviceRegistrationMapper;

    @Transactional
    public ServiceRegistrationResponse registerService(ServiceRegistrationRequest request) {
        User currentUser = getCurrentUser();

        GymService gymService = gymServiceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new RuntimeException("Service not found"));

        if (!gymService.getIsActive()) {
            throw new RuntimeException("Service is not active");
        }

        if (registrationRepository.existsByUserAndGymServiceAndStatus(
                currentUser, gymService, RegistrationStatus.ACTIVE)
            || registrationRepository.existsByUserAndGymServiceAndStatus(
                currentUser, gymService, RegistrationStatus.PENDING)) {
            throw new RuntimeException("You have already registered for this service");
        }
        ServiceRegistration registration = new ServiceRegistration();
        registration.setUser(currentUser);
        registration.setGymService(gymService);
        registration.setNotes(request.getNotes());
        RegistrationType regType = request.getRegistrationType() != null 
            ? request.getRegistrationType() 
            : RegistrationType.ONLINE;
        registration.setRegistrationType(regType);

        registration.setStatus(RegistrationStatus.PENDING);

        if (request.getTrainerId() != null) {
            User trainer = userRepository.findById(request.getTrainerId())
                    .orElseThrow(() -> new RuntimeException("Trainer not found"));
            registration.setTrainer(trainer);
            registration.setTrainerSelectedAt(java.time.LocalDateTime.now());
        }

        ServiceRegistration saved = registrationRepository.save(registration);
        

        
        ServiceRegistrationResponse response = mapToResponse(saved);
        eventPublisher.publishEvent(
            new EntityChangedEvent(this, "SERVICE_REGISTRATION", "REGISTERED", response, saved.getId())
        );
        
        // Send email notification for counter registrations
        if (saved.getRegistrationType() == RegistrationType.COUNTER) {
            sendCounterRegistrationEmail(saved);
        }
        
        return response;
    }

    @Transactional
    public void cancelRegistration(Long registrationId) {
        User currentUser = getCurrentUser();
        
        ServiceRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Registration not found"));
        if (!registration.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You can only cancel your own registration");
        }
        if (registration.getStatus() != RegistrationStatus.ACTIVE) {
            throw new RuntimeException("Registration is not active");
        }
        registration.setStatus(RegistrationStatus.CANCELLED);
        registration.setCancelledDate(LocalDateTime.now());
        registrationRepository.save(registration);
    }

    @Transactional(readOnly = true)
    public List<ServiceRegistrationResponse> getMyRegistrations() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("User not authenticated");
        }
        return registrationRepository.findByUserWithGymServiceOrderByRegistrationDateDesc(currentUser)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServiceRegistrationResponse> getServiceRegistrations(Long serviceId) {
        GymService gymService = gymServiceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));
        
        return registrationRepository.findByGymServiceWithUserOrderByRegistrationDateDesc(gymService)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ServiceRegistrationResponse> getServiceRegistrations(Long serviceId, int page, int size) {
        GymService gymService = gymServiceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("registrationDate").descending());
        Page<ServiceRegistration> registrationPage = registrationRepository.findByGymServiceWithUser(gymService, pageable);
        return registrationPage.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<ServiceRegistrationResponse> getAllRegistrations() {
        return registrationRepository.findAllWithUserAndGymService()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ServiceRegistrationResponse> getAllRegistrations(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("registrationDate").descending());
        Page<ServiceRegistration> registrationPage = registrationRepository.findAllWithUserAndGymService(pageable);
        return registrationPage.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ServiceRegistrationResponse> getAllRegistrationsWithFilters(ServiceRegistrationFilterRequest request) {
        Specification<ServiceRegistration> spec = buildSpecification(request);
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by("registrationDate").descending());
        Page<ServiceRegistration> registrationPage = registrationRepository.findAll(spec, pageable);
        
        // Force initialization of lazy-loaded relationships within transaction
        registrationPage.getContent().forEach(reg -> {
            reg.getUser().getFullName(); // Initialize user
            reg.getGymService().getName(); // Initialize gymService
            if (reg.getTrainer() != null) {
                reg.getTrainer().getFullName(); // Initialize trainer
            }
        });
        
        return registrationPage.map(this::mapToResponse);
    }

    private Specification<ServiceRegistration> buildSpecification(ServiceRegistrationFilterRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by status
            if (request.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), request.getStatus()));
            }

            // Filter by registrationType
            if (request.getRegistrationType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("registrationType"), request.getRegistrationType()));
            }

            // Filter by paymentStatus - requires checking PaymentOrder table
            if (request.getPaymentStatus() != null) {
                // Use EXISTS subquery to check if user has payment with matching status for this service
                var subquery = query.subquery(PaymentOrder.class);
                var paymentRoot = subquery.from(PaymentOrder.class);
                
                subquery.select(paymentRoot);
                subquery.where(
                    criteriaBuilder.and(
                        criteriaBuilder.equal(paymentRoot.get("user"), root.get("user")),
                        criteriaBuilder.equal(paymentRoot.get("itemType"), "SERVICE"),
                        criteriaBuilder.equal(
                            criteriaBuilder.toString(root.get("gymService").get("id")),
                            paymentRoot.get("itemId")
                        ),
                        criteriaBuilder.equal(paymentRoot.get("status"), request.getPaymentStatus())
                    )
                );
                predicates.add(criteriaBuilder.exists(subquery));
            }

            // Search by member name OR email OR service name (case-insensitive partial match)
            if (request.getSearchQuery() != null && !request.getSearchQuery().trim().isEmpty()) {
                String searchPattern = "%" + request.getSearchQuery().toLowerCase() + "%";
                
                // Join with User (member) and GymService
                Join<ServiceRegistration, User> userJoin = root.join("user", JoinType.INNER);
                Join<ServiceRegistration, GymService> serviceJoin = root.join("gymService", JoinType.INNER);
                
                Predicate memberNamePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(userJoin.get("fullName")), 
                    searchPattern
                );
                Predicate memberEmailPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(userJoin.get("email")), 
                    searchPattern
                );
                Predicate serviceNamePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(serviceJoin.get("name")), 
                    searchPattern
                );
                
                predicates.add(criteriaBuilder.or(memberNamePredicate, memberEmailPredicate, serviceNamePredicate));
            }

            // Ensure distinct results to avoid duplicates from joins
            query.distinct(true);

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Transactional(readOnly = true)
    public ServiceRegistrationWithTrainerSelectionResponse getRegistrationForTrainerSelection(Long registrationId) {
        User currentUser = getCurrentUser();
        
        ServiceRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Registration not found"));

        // Verify ownership
        if (!registration.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You can only access your own registration");
        }

        if (registration.getStatus() != RegistrationStatus.ACTIVE) {
            throw new RuntimeException("Registration is not active");
        }

        // Get available trainers for this service
        List<TrainerForBookingResponse> availableTrainers =
            enhancedServiceRegistrationService.getAvailableTrainers(registration.getGymService().getId());

        // Check if trainer is already selected
        boolean hasSelectedTrainer = registration.getTrainer() != null;
        
        // Check if there's an active booking
        List<TrainerBooking> activeBookings = trainerBookingRepository
            .findByServiceRegistration_Id(registrationId);
        
        TrainerBooking activeBooking = activeBookings.stream()
            .filter(booking -> booking.getStatus() == BookingStatus.PENDING ||
                             booking.getStatus() == BookingStatus.CONFIRMED)
            .findFirst()
            .orElse(null);

        return ServiceRegistrationWithTrainerSelectionResponse.builder()
            .registrationId(registration.getId())
            .serviceName(registration.getGymService().getName())
            .serviceDescription(registration.getGymService().getDescription())
            .serviceCategory(registration.getGymService().getCategory().getName())
            .servicePrice(registration.getGymService().getPrice().doubleValue())
            .registrationDate(registration.getRegistrationDate())
            .registrationStatus(registration.getStatus().name())
            .userId(currentUser.getId())
            .userFullName(currentUser.getFullName())
            .userEmail(currentUser.getEmail())
            .userPhone(currentUser.getPhoneNumber())
            .availableTrainers(availableTrainers)
            .totalAvailableTrainers(availableTrainers.size())
            .hasSelectedTrainer(hasSelectedTrainer)
            .selectedTrainerId(hasSelectedTrainer ? registration.getTrainer().getId() : null)
            .selectedTrainerName(hasSelectedTrainer ? registration.getTrainer().getFullName() : null)
            .trainerSelectedAt(registration.getTrainerSelectedAt())
            .hasActiveBooking(activeBooking != null)
            .activeBookingId(activeBooking != null ? activeBooking.getId() : null)
            .bookingStatus(activeBooking != null ? activeBooking.getStatus().name() : null)
            .build();
    }

    @Transactional(readOnly = true)
    public List<AvailableTrainerResponse> getAvailableTrainersForRegistration(Long registrationId) {
        // Fetch ServiceRegistration by ID
        ServiceRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Registration not found"));

        // Get the service category from registration.getGymService().getCategory()
        ServiceCategory serviceCategory = registration.getGymService().getCategory();

        // Query trainers who have specialty in that service category
        List<TrainerSpecialty> trainerSpecialties = trainerSpecialtyRepository
                .findTrainerSpecialtiesByCategory(serviceCategory.getId());

        // Group by trainer and map to AvailableTrainerResponse
        Map<Long, List<TrainerSpecialty>> specialtiesByTrainer = trainerSpecialties.stream()
                .collect(Collectors.groupingBy(ts -> ts.getUser().getId()));

        return specialtiesByTrainer.entrySet().stream()
                .map(entry -> {
                    User trainer = entry.getValue().get(0).getUser();
                    List<TrainerSpecialty> specialties = entry.getValue();

                    // Get specialty names
                    List<String> specialtyNames = specialties.stream()
                            .map(ts -> ts.getSpecialty().getName())
                            .collect(Collectors.toList());

                    // Calculate total experience years
                    Integer totalExperienceYears = specialties.stream()
                            .map(TrainerSpecialty::getExperienceYears)
                            .filter(years -> years != null)
                            .reduce(0, Integer::sum);

                    return AvailableTrainerResponse.builder()
                            .id(trainer.getId())
                            .fullName(trainer.getFullName())
                            .avatar(trainer.getAvatar())
                            .specialtyNames(specialtyNames)
                            .totalExperienceYears(totalExperienceYears)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private User getCurrentUser() {
        return SecurityUtils.getCurrentUser();
    }

    public java.util.Map<String, java.util.List<String>> getTrainerBookedSlots(Long trainerId, java.time.LocalDate date) {
        List<TrainerBooking> bookings = trainerBookingRepository
            .findByTrainerIdAndBookingDateAndStatus(trainerId, date, BookingStatus.PENDING);
        List<TrainerBooking> confirmed = trainerBookingRepository
            .findByTrainerIdAndBookingDateAndStatus(trainerId, date, BookingStatus.CONFIRMED);

        java.util.List<String> bookedSlots = new java.util.ArrayList<>();
        for (TrainerBooking b : bookings) {
            bookedSlots.add(b.getStartTime().toString() + "-" + b.getEndTime().toString());
        }
        for (TrainerBooking b : confirmed) {
            bookedSlots.add(b.getStartTime().toString() + "-" + b.getEndTime().toString());
        }

        java.util.Map<String, java.util.List<String>> result = new java.util.HashMap<>();
        result.put("bookedSlots", bookedSlots);
        return result;
    }

    private ServiceRegistrationResponse mapToResponse(ServiceRegistration registration) {
        return serviceRegistrationMapper.toResponse(registration);
    }

    // Send counter registration email notification
    private void sendCounterRegistrationEmail(ServiceRegistration registration) {
        try {
            String userEmail = registration.getUser().getEmail();
            String fullName = registration.getUser().getFullName();
            String registrationId = registration.getId().toString();
            String serviceName = registration.getGymService().getName();
            String amount = String.format("%,d", registration.getGymService().getPrice().longValue());
            
            log.info("Sending counter registration email to {} for registration {}", userEmail, registrationId);
            
            // Send email asynchronously
            emailService.sendCounterRegistrationEmailAsync(
                userEmail, 
                fullName, 
                registrationId, 
                serviceName, 
                amount
            ).thenAccept(success -> {
                if (success) {
                    log.info("Counter registration email sent successfully to {} for registration {}", 
                        userEmail, registrationId);
                } else {
                    log.error("Failed to send counter registration email to {} for registration {}", 
                        userEmail, registrationId);
                }
            });
            
        } catch (Exception e) {
            log.error("Error sending counter registration email for registration {} - continuing", 
                registration.getId(), e);
            // Don't fail the registration if email fails
        }
    }

}
