package com.example.project_backend04.dto.response.Trainer;

import com.example.project_backend04.dto.response.TrainerBooking.TimeSlot;
import com.example.project_backend04.dto.response.User.UserResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerAvailabilityDTO {
    
    private UserResponse trainer;
    private List<TrainerSpecialtyResponse> specialties;
    private List<TimeSlot> availableSlots;
    private Integer totalExperience;
    private Double averageRating;
    private Integer completedSessions;
    private Integer totalBookings;
    private String bio;
    private String education;
    private List<String> certifications;

    @JsonProperty("isAvailable")
    private Boolean available;  // renamed from isAvailable to avoid Lombok/Jackson conflict

    private String unavailabilityReason;

    public boolean isAvailable() {
        return Boolean.TRUE.equals(available);
    }
    
    public boolean hasAvailableSlots() {
        return availableSlots != null && !availableSlots.isEmpty();
    }
    
    public int getAvailableSlotsCount() {
        return availableSlots != null ? availableSlots.size() : 0;
    }
    
    public boolean hasSpecialties() {
        return specialties != null && !specialties.isEmpty();
    }
    
    public boolean hasRating() {
        return averageRating != null && averageRating > 0;
    }
    
    public String getFormattedRating() {
        if (hasRating()) {
            return String.format("%.1f", averageRating);
        }
        return "No rating";
    }
    
    public String getExperienceLevel() {
        if (totalExperience == null || totalExperience == 0) {
            return "New trainer";
        } else if (totalExperience < 2) {
            return "Junior trainer";
        } else if (totalExperience < 5) {
            return "Experienced trainer";
        } else {
            return "Senior trainer";
        }
    }
}