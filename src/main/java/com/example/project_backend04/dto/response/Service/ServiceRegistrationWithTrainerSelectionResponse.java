package com.example.project_backend04.dto.response.Service;

import com.example.project_backend04.dto.response.Trainer.TrainerAvailabilityDTO;
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
    
    // Service Registration Info
    private Long registrationId;
    private String serviceName;
    private String serviceDescription;
    private String serviceCategory;
    private Double servicePrice;
    private LocalDateTime registrationDate;
    private String registrationStatus;
    
    // User Info
    private Long userId;
    private String userFullName;
    private String userEmail;
    private String userPhone;
    
    // Available Trainers
    private List<TrainerAvailabilityDTO> availableTrainers;
    private int totalAvailableTrainers;
    
    // Trainer Selection Status
    private boolean hasSelectedTrainer;
    private Long selectedTrainerId;
    private String selectedTrainerName;
    private LocalDateTime trainerSelectedAt;
    
    // Booking Status
    private boolean hasActiveBooking;
    private Long activeBookingId;
    private String bookingStatus;
    
    // Helper methods
    public boolean canSelectTrainer() {
        return !hasSelectedTrainer && availableTrainers != null && !availableTrainers.isEmpty();
    }
    
    public boolean needsTrainerSelection() {
        return !hasSelectedTrainer && !hasActiveBooking;
    }
    
    public String getSelectionMessage() {
        if (hasActiveBooking) {
            return "Đã có lịch hẹn với trainer";
        } else if (hasSelectedTrainer) {
            return "Đã chọn trainer: " + selectedTrainerName;
        } else if (availableTrainers == null || availableTrainers.isEmpty()) {
            return "Hiện tại chưa có trainer phù hợp";
        } else {
            return "Vui lòng chọn trainer để đặt lịch";
        }
    }
}