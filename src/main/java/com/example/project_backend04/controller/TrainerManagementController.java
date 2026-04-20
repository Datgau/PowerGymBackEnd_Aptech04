package com.example.project_backend04.controller;

import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.dto.response.Trainer.TrainerBookingResponse;
import com.example.project_backend04.dto.response.Trainer.TrainerScheduleResponse;
import com.example.project_backend04.dto.response.Trainer.TrainerStatisticsResponse;
import com.example.project_backend04.service.TrainerManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trainer-management")
@RequiredArgsConstructor
@Slf4j
public class TrainerManagementController {
    
    private final TrainerManagementService trainerManagementService;

    @GetMapping("/trainer/{trainerId}/schedule")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF') or (hasRole('TRAINER') and @securityUtils.isCurrentUser(#trainerId))")
    public ResponseEntity<ApiResponse<TrainerScheduleResponse>> getTrainerSchedule(
            @PathVariable Long trainerId,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        try {
            LocalDate startDate = fromDate != null ? fromDate : LocalDate.now();
            LocalDate endDate = toDate != null ? toDate : startDate.plusDays(7);
            
            TrainerScheduleResponse schedule = 
                trainerManagementService.getTrainerSchedule(trainerId, startDate, endDate);

            return ResponseEntity.ok(ApiResponse.<TrainerScheduleResponse>builder()
                    .success(true)
                    .message("Successfully retrieved trainer schedule")
                    .data(schedule)
                    .status(HttpStatus.OK.value())
                    .build());
                
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<TrainerScheduleResponse>builder()
                            .success(false)
                            .message("Error while retrieving schedule: " + e.getMessage())
                            .status(HttpStatus.BAD_REQUEST.value())
                            .build());
        }
    }

    @GetMapping("/trainer/{trainerId}/pending-requests")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF') or (hasRole('TRAINER') and @securityUtils.isCurrentUser(#trainerId))")
    public ResponseEntity<ApiResponse<List<TrainerBookingResponse>>> getPendingBookingRequests(
            @PathVariable Long trainerId) {
        try {
            List<TrainerBookingResponse> pendingRequests =
                    trainerManagementService.getPendingBookingRequests(trainerId);

            String message = pendingRequests.isEmpty() ?
                    "No pending booking requests" :
                    String.format("There are %d pending booking requests", pendingRequests.size());

            return ResponseEntity.ok(ApiResponse.<List<TrainerBookingResponse>>builder()
                    .success(true)
                    .message(message)
                    .data(pendingRequests)
                    .status(HttpStatus.OK.value())
                    .build());

        } catch (Exception e) {
            log.error("Error getting pending requests for trainer {}", trainerId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<List<TrainerBookingResponse>>builder()
                            .success(false)
                            .message("Error while fetching requests: " + e.getMessage())
                            .status(HttpStatus.BAD_REQUEST.value())
                            .build());
        }
    }

    @GetMapping("/trainer/{trainerId}/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF') or (hasRole('TRAINER') and @securityUtils.isCurrentUser(#trainerId))")
    public ResponseEntity<ApiResponse<TrainerStatisticsResponse>> getTrainerStatistics(
            @PathVariable Long trainerId,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        try {
            // Default to current month if no dates provided
            LocalDate startDate = fromDate != null ? fromDate : LocalDate.now().withDayOfMonth(1);
            LocalDate endDate = toDate != null ? toDate : LocalDate.now();

            TrainerStatisticsResponse statistics =
                    trainerManagementService.getTrainerStatistics(trainerId, startDate, endDate);

            return ResponseEntity.ok(ApiResponse.<TrainerStatisticsResponse>builder()
                    .success(true)
                    .message("Successfully retrieved trainer statistics")
                    .data(statistics)
                    .status(HttpStatus.OK.value())
                    .build());

        } catch (Exception e) {
            log.error("Error getting trainer statistics for trainer {}", trainerId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<TrainerStatisticsResponse>builder()
                            .success(false)
                            .message("Error while fetching statistics: " + e.getMessage())
                            .status(HttpStatus.BAD_REQUEST.value())
                            .build());
        }
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<TrainerScheduleResponse>>> getTrainersOverview(
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDate targetDate = date != null ? date : LocalDate.now();
        try {
            List<TrainerScheduleResponse> trainersOverview =
                    trainerManagementService.getTrainersOverview(targetDate);

            return ResponseEntity.ok(ApiResponse.<List<TrainerScheduleResponse>>builder()
                    .success(true)
                    .message(String.format("Successfully retrieved overview of %d trainers", trainersOverview.size()))
                    .data(trainersOverview)
                    .status(HttpStatus.OK.value())
                    .build());

        } catch (Exception e) {
            log.error("Error getting trainers overview for date {}", targetDate, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<List<TrainerScheduleResponse>>builder()
                            .success(false)
                            .message("Error while retrieving trainer overview: " + e.getMessage())
                            .status(HttpStatus.BAD_REQUEST.value())
                            .build());
        }
    }

    @GetMapping("/workload-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<TrainerStatisticsResponse>>> getWorkloadSummary(
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        try {
            // Default to current month if no dates provided
            LocalDate startDate = fromDate != null ? fromDate : LocalDate.now().withDayOfMonth(1);
            LocalDate endDate = toDate != null ? toDate : LocalDate.now();

            List<TrainerStatisticsResponse> workloadSummary =
                    trainerManagementService.getWorkloadSummary(startDate, endDate);

            return ResponseEntity.ok(ApiResponse.<List<TrainerStatisticsResponse>>builder()
                    .success(true)
                    .message("Successfully retrieved workload summary")
                    .data(workloadSummary)
                    .status(HttpStatus.OK.value())
                    .build());

        } catch (Exception e) {
            log.error("Error getting workload summary", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<List<TrainerStatisticsResponse>>builder()
                            .success(false)
                            .message("Error while retrieving workload summary: " + e.getMessage())
                            .status(HttpStatus.BAD_REQUEST.value())
                            .build());
        }
    }
}