package com.example.project_backend04.dto.response.TrainerBooking;

import com.example.project_backend04.entity.TrainerBooking;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConflictCheckResult {
    
    private boolean hasConflict;
    private List<TrainerBooking> conflictingBookings;
    private String conflictMessage;
    private List<TimeSlot> suggestedAlternatives;
    private String validationMessage;
    private boolean isValid;
    
    public static ConflictCheckResult noConflict() {
        return ConflictCheckResult.builder()
            .hasConflict(false)
            .isValid(true)
            .build();
    }
    
    public static ConflictCheckResult conflict(List<TrainerBooking> conflictingBookings, String message) {
        return ConflictCheckResult.builder()
            .hasConflict(true)
            .conflictingBookings(conflictingBookings)
            .conflictMessage(message)
            .isValid(false)
            .build();
    }
    
    public static ConflictCheckResult invalid(String validationMessage) {
        return ConflictCheckResult.builder()
            .hasConflict(false)
            .isValid(false)
            .validationMessage(validationMessage)
            .build();
    }
    
    public ConflictCheckResult withAlternatives(List<TimeSlot> alternatives) {
        this.suggestedAlternatives = alternatives;
        return this;
    }
}