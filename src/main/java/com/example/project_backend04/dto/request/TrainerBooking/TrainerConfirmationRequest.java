package com.example.project_backend04.dto.request.TrainerBooking;

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
public class TrainerConfirmationRequest {
    
    @NotNull(message = "Confirmation status is required")
    private boolean confirmed;
    
    @Size(max = 1000, message = "Trainer notes cannot exceed 1000 characters")
    private String trainerNotes;
    
    @Size(max = 500, message = "Rejection reason cannot exceed 500 characters")
    private String rejectionReason;
    
    public boolean isConfirmed() {
        return confirmed;
    }
    
    public boolean isRejected() {
        return !confirmed;
    }
    
    public boolean hasTrainerNotes() {
        return trainerNotes != null && !trainerNotes.trim().isEmpty();
    }
    
    public boolean hasRejectionReason() {
        return rejectionReason != null && !rejectionReason.trim().isEmpty();
    }
}