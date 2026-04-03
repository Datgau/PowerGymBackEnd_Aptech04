package com.example.project_backend04.dto.request.Service;

import com.example.project_backend04.enums.PaymentStatus;
import com.example.project_backend04.enums.RegistrationStatus;
import com.example.project_backend04.enums.RegistrationType;
import lombok.Data;

@Data
public class ServiceRegistrationFilterRequest {
    private RegistrationStatus status;
    private PaymentStatus paymentStatus;
    private RegistrationType registrationType;
    private String searchQuery;
    private int page = 0;
    private int size = 10;
}
