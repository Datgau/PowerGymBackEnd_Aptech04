package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.Service.ServiceRegistrationRequest;
import com.example.project_backend04.dto.response.Service.GymServiceResponse;
import com.example.project_backend04.dto.response.Service.ServiceRegistrationResponse;
import com.example.project_backend04.dto.response.User.UserResponse;
import com.example.project_backend04.entity.GymService;
import com.example.project_backend04.entity.ServiceRegistration;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.event.EntityChangedEvent;
import com.example.project_backend04.mapper.UserMapper;
import com.example.project_backend04.repository.GymServiceRepository;
import com.example.project_backend04.repository.ServiceRegistrationRepository;
import com.example.project_backend04.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceRegistrationService {

    private final ServiceRegistrationRepository registrationRepository;
    private final GymServiceRepository gymServiceRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final GymServiceService gymServiceService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ServiceRegistrationResponse registerService(ServiceRegistrationRequest request) {
        User currentUser = getCurrentUser();
        
        GymService gymService = gymServiceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new RuntimeException("Service not found"));

        if (!gymService.getIsActive()) {
            throw new RuntimeException("Service is not active");
        }
        if (registrationRepository.existsByUserAndGymServiceAndStatus(
                currentUser, gymService, ServiceRegistration.RegistrationStatus.ACTIVE)) {
            throw new RuntimeException("Bạn đã đăng ký service này rồi");
        }

        ServiceRegistration registration = new ServiceRegistration();
        registration.setUser(currentUser);
        registration.setGymService(gymService);
        registration.setNotes(request.getNotes());
        registration.setStatus(ServiceRegistration.RegistrationStatus.ACTIVE);

        ServiceRegistration saved = registrationRepository.save(registration);
        ServiceRegistrationResponse response = mapToResponse(saved);
        eventPublisher.publishEvent(
            new EntityChangedEvent(this, "SERVICE_REGISTRATION", "REGISTERED", response, saved.getId())
        );
        
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

        if (registration.getStatus() != ServiceRegistration.RegistrationStatus.ACTIVE) {
            throw new RuntimeException("Registration is not active");
        }

        registration.setStatus(ServiceRegistration.RegistrationStatus.CANCELLED);
        registration.setCancelledDate(LocalDateTime.now());
        registrationRepository.save(registration);
    }

    @Transactional(readOnly = true)
    public List<ServiceRegistrationResponse> getMyRegistrations() {
        User currentUser = getCurrentUser();
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

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private ServiceRegistrationResponse mapToResponse(ServiceRegistration registration) {
        UserResponse userResponse = userMapper.toResponse(registration.getUser());
        GymServiceResponse serviceResponse = gymServiceService.getServiceById(registration.getGymService().getId());

        return ServiceRegistrationResponse.builder()
                .id(registration.getId())
                .user(userResponse)
                .service(serviceResponse)
                .status(registration.getStatus())
                .notes(registration.getNotes())
                .registrationDate(registration.getRegistrationDate())
                .cancelledDate(registration.getCancelledDate())
                .cancellationReason(registration.getCancellationReason())
                .build();
    }
}
