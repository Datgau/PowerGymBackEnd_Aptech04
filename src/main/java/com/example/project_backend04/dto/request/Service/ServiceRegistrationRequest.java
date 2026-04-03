package com.example.project_backend04.dto.request.Service;

import com.example.project_backend04.enums.RegistrationType;
import lombok.Data;

@Data
public class ServiceRegistrationRequest {
    private Long serviceId;
    private String notes;
    private RegistrationType registrationType; // ONLINE hoặc COUNTER
}
