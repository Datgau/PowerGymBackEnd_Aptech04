package com.example.project_backend04.dto.response.Service;

import com.example.project_backend04.dto.response.Trainer.TrainerForBookingResponse;
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
public class ServiceRegistrationWithTrainerSelectionResponse {
    
    private Long registrationId;
    private String serviceName;
    private String serviceDescription;
    private String serviceCategory;
    private Double servicePrice;
    private LocalDateTime registrationDate;
    private String registrationStatus;
    
    private Long userId;
    private String userFullName;
    private String userEmail;
    private String userPhone;
    
    private List<TrainerForBookingResponse> availableTrainers;
    private int totalAvailableTrainers;
    
    private boolean hasSelectedTrainer;
    private Long selectedTrainerId;
    private String selectedTrainerName;
    private LocalDateTime trainerSelectedAt;
    
    private boolean hasActiveBooking;
    private Long activeBookingId;
    private String bookingStatus;
    
    public boolean needsTrainerSelection() {
        return !hasSelectedTrainer && !hasActiveBooking;
    }
}