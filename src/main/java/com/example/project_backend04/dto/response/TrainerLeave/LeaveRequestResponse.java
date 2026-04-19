package com.example.project_backend04.dto.response.TrainerLeave;

import com.example.project_backend04.enums.LeaveRequestStatus;
import com.example.project_backend04.enums.LeaveRequestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequestResponse {
    
    private Long id;
    private Long trainerId;
    private String trainerName;
    private String trainerEmail;
    private String trainerAvatar;
    private LocalDate leaveDate;
    private LeaveRequestType leaveType;
    private LocalTime startTime;
    private LocalTime endTime;
    private String reason;
    private LeaveRequestStatus status;
    private String adminNotes;
    private Long reviewedBy;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
