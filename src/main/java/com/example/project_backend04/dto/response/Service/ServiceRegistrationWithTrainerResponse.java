package com.example.project_backend04.dto.response.Service;

import com.example.project_backend04.dto.response.TrainerBooking.TrainerBookingResponse;
import com.example.project_backend04.entity.ServiceRegistration;
import com.example.project_backend04.enums.RegistrationStatus;
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
public class ServiceRegistrationWithTrainerResponse {
    
    private Long id;
    private Long userId;
    private String userName;
    private Long serviceId;
    private String serviceName;
    private String serviceDescription;
    private Long trainerId;
    private String trainerName;
    private String trainerAvatar;
    private String trainerBio;
    private RegistrationStatus status;
    private LocalDateTime registrationDate;
    private LocalDateTime trainerSelectedAt;
    private String trainerSelectionNotes;
    private String notes;
    private boolean hasTrainer;
    private boolean canBookTrainer;
    private int totalBookings;
    private List<TrainerBookingResponse> upcomingBookings;
    private List<TrainerBookingResponse> recentBookings;
    
    public boolean isActive() {
        return status == RegistrationStatus.ACTIVE;
    }
    
    public boolean canAssignTrainer() {
        return isActive() && !hasTrainer;
    }
    
    public boolean canRemoveTrainer() {
        return isActive() && hasTrainer;
    }
    
    public String getStatusDisplay() {
        if (status == null) return "Unknown";
        
        switch (status) {
            case ACTIVE: return "Active";
            case CANCELLED: return "Cancelled";
            case COMPLETED: return "Completed";
            default: return status.name();
        }
    }
    
    public String getTrainerStatusDisplay() {
        if (!hasTrainer) {
            return "No trainer assigned";
        }
        
        if (canBookTrainer) {
            return "Ready for booking";
        }
        
        return "Trainer assigned";
    }
}