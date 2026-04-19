package com.example.project_backend04.mapper;

import com.example.project_backend04.dto.response.Service.GymServiceResponse;
import com.example.project_backend04.dto.response.Service.ServiceRegistrationResponse;
import com.example.project_backend04.dto.response.User.UserResponse;
import com.example.project_backend04.entity.ServiceRegistration;
import com.example.project_backend04.enums.PaymentStatus;
import com.example.project_backend04.enums.RegistrationType;
import com.example.project_backend04.service.GymServiceService;
import com.example.project_backend04.service.TrainerBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServiceRegistrationMapper {

    private final UserMapper userMapper;
    private final GymServiceService gymServiceService;
    private final TrainerBookingService trainerBookingService;

    public ServiceRegistrationResponse toResponse(ServiceRegistration registration) {
        UserResponse userResponse = userMapper.toResponse(registration.getUser());
        GymServiceResponse serviceResponse = gymServiceService.getServiceById(registration.getGymService().getId());
        
        // Get payment status directly from the linked PaymentOrder on this registration.
        // Do NOT query by user+service — that would incorrectly pick up payments from
        // other registrations of the same service by the same user.
        PaymentStatus paymentStatus;
        if (registration.getPaymentOrder() != null) {
            paymentStatus = registration.getPaymentOrder().getStatus();
        } else {
            paymentStatus = PaymentStatus.PENDING;
        }

        // Populate trainerName from assigned trainer
        String trainerName = null;
        Long trainerId = null;
        if (registration.getTrainer() != null) {
            trainerName = registration.getTrainer().getFullName();
            trainerId = registration.getTrainer().getId();
        }

        // Populate registrationType from entity
        RegistrationType registrationType = registration.getRegistrationType();

        // Populate paymentOrderId from payment order
        String paymentOrderId = null;
        if (registration.getPaymentOrder() != null) {
            paymentOrderId = registration.getPaymentOrder().getId();
        }
        
        // Fetch all bookings for this registration (including REJECTED, PENDING, etc.)
        var bookings = trainerBookingService.getBookingsByServiceRegistration(registration.getId());

        return ServiceRegistrationResponse.builder()
                .id(registration.getId())
                .user(userResponse)
                .service(serviceResponse)
                .status(registration.getActualStatus())
                .notes(registration.getNotes())
                .registrationDate(registration.getRegistrationDate())
                .expirationDate(registration.getExpirationDate())
                .cancelledDate(registration.getCancelledDate())
                .cancellationReason(registration.getCancellationReason())
                .paymentStatus(paymentStatus)
                .trainerName(trainerName)
                .trainerId(trainerId)
                .registrationType(registrationType)
                .paymentOrderId(paymentOrderId)
                .upcomingBookings(bookings) // Include all bookings with all statuses
                .build();
    }
}

