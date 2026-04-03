package com.example.project_backend04.dto.response.Service;

import com.example.project_backend04.dto.response.User.UserResponse;
import com.example.project_backend04.entity.ServiceRegistration;
import com.example.project_backend04.enums.PaymentStatus;
import com.example.project_backend04.enums.RegistrationStatus;
import com.example.project_backend04.enums.RegistrationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRegistrationResponse {
    private Long id;
    private UserResponse user;
    private GymServiceResponse service;
    private RegistrationStatus status;
    private String notes;
    private LocalDateTime registrationDate;
    private LocalDateTime expirationDate;
    private LocalDateTime cancelledDate;
    private String cancellationReason;
    private PaymentStatus paymentStatus;
    private String trainerName;
    private RegistrationType registrationType;
}
