package com.example.project_backend04.dto.response.Payment;

import com.example.project_backend04.dto.response.Service.ServiceRegistrationWithTrainerSelectionResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentWithTrainerSelectionResponse {
    
    // Payment Information
    private String orderId;
    private String transactionId;
    private Double amount;
    private String status; // PENDING, COMPLETED, FAILED, CANCELLED
    private String paymentMethod;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    
    // User Information
    private Long userId;
    private String userFullName;
    private String userEmail;
    
    // Service Registrations (that can have trainer selection)
    private List<ServiceRegistrationWithTrainerSelectionResponse> serviceRegistrations;
    private int totalRegistrations;
    private int registrationsNeedingTrainer;
    
    // Workflow Status
    private boolean paymentCompleted;
    private boolean hasServiceRegistrations;
    private boolean needsTrainerSelection;
    private String nextAction; // "SELECT_TRAINER", "BOOK_SESSION", "COMPLETE"
    

}