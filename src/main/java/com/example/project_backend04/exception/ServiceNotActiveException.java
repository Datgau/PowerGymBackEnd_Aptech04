package com.example.project_backend04.exception;

public class ServiceNotActiveException extends BankPaymentException {
    
    private final Long serviceId;
    
    public ServiceNotActiveException(Long serviceId) {
        super("Service is not active: " + serviceId, "SERVICE_NOT_ACTIVE");
        this.serviceId = serviceId;
    }
    
    public Long getServiceId() {
        return serviceId;
    }
}