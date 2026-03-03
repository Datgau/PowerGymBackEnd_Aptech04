package com.example.project_backend04.dto.request.Service;

import lombok.Data;

@Data
public class ServiceRegistrationRequest {
    private Long serviceId;
    private String notes;
}
