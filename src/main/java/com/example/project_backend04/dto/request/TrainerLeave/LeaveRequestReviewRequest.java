package com.example.project_backend04.dto.request.TrainerLeave;

import com.example.project_backend04.enums.LeaveRequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequestReviewRequest {
    
    @NotNull(message = "Status is required")
    private LeaveRequestStatus status; // APPROVED or REJECTED
    
    private String adminNotes;
}
