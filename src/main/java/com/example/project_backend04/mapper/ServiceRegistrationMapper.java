package com.example.project_backend04.mapper;

import com.example.project_backend04.dto.response.Service.GymServiceResponse;
import com.example.project_backend04.dto.response.Service.ServiceRegistrationResponse;
import com.example.project_backend04.dto.response.User.UserResponse;
import com.example.project_backend04.entity.PaymentOrder;
import com.example.project_backend04.entity.ServiceRegistration;
import com.example.project_backend04.enums.PaymentStatus;
import com.example.project_backend04.enums.RegistrationType;
import com.example.project_backend04.repository.PaymentOrderRepository;
import com.example.project_backend04.service.GymServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ServiceRegistrationMapper {

    private final UserMapper userMapper;
    private final GymServiceService gymServiceService;
    private final PaymentOrderRepository paymentOrderRepository;

    public ServiceRegistrationResponse toResponse(ServiceRegistration registration) {
        UserResponse userResponse = userMapper.toResponse(registration.getUser());
        GymServiceResponse serviceResponse = gymServiceService.getServiceById(registration.getGymService().getId());
        
        // Get payment status from payment orders
        PaymentStatus paymentStatus = null;
        List<PaymentOrder> paymentOrders = paymentOrderRepository.findByUserAndItemTypeAndItemIdOrderByCreatedAtDesc(
            registration.getUser(),
            "SERVICE", 
            registration.getGymService().getId().toString()
        );
        
        if (!paymentOrders.isEmpty()) {
            // First try to find a SUCCESS payment
            PaymentOrder successPayment = paymentOrders.stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .findFirst()
                .orElse(null);
            
            if (successPayment != null) {
                paymentStatus = successPayment.getStatus();
            } else {
                // If no SUCCESS payment, use the latest one
                paymentStatus = paymentOrders.get(0).getStatus();
            }
        } else {
            // If no PaymentOrder, set PENDING
            paymentStatus = PaymentStatus.PENDING;
        }

        // Populate trainerName from assigned trainer
        String trainerName = null;
        if (registration.getTrainer() != null) {
            trainerName = registration.getTrainer().getFullName();
        }

        // Populate registrationType from entity
        RegistrationType registrationType = registration.getRegistrationType();

        // Populate paymentOrderId from payment order
        String paymentOrderId = null;
        if (registration.getPaymentOrder() != null) {
            paymentOrderId = registration.getPaymentOrder().getId();
        }

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
                .registrationType(registrationType)
                .paymentOrderId(paymentOrderId)
                .build();
    }
}
