package com.example.project_backend04.dto.request.Service;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRegistrationWithTrainerRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Service ID is required")
    private Long serviceId;
    
    private Long trainerId; // Optional - can be assigned later
    
    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;
    
    @Size(max = 500, message = "Trainer selection notes cannot exceed 500 characters")
    private String trainerSelectionNotes;
    
    public boolean hasTrainerSelected() {
        return trainerId != null;
    }
}