package com.example.project_backend04.dto.request.TrainerLeave;

import com.example.project_backend04.enums.LeaveRequestType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequestCreateRequest {
    
    @NotNull(message = "Leave date is required")
    private LocalDate leaveDate;
    
    @NotNull(message = "Leave type is required")
    private LeaveRequestType leaveType;
    
    // Required for TIME_SLOT type
    private LocalTime startTime;
    
    // Required for TIME_SLOT type
    private LocalTime endTime;
    
    private String reason;
}
