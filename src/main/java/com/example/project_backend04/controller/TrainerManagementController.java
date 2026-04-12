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

    /**
     * Get trainer's schedule for a specific date range
     */
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
                .message("Lấy lịch trình trainer thành công")
                .data(schedule)
                .status(HttpStatus.OK.value())
                .build());
                
        } catch (Exception e) {
            log.error("Error getting trainer schedule for trainer {}", trainerId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<TrainerScheduleResponse>builder()
                    .success(false)
                    .message("Lỗi khi lấy lịch trình: " + e.getMessage())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }

    /**
     * Get pending booking requests for a trainer
     */
    @GetMapping("/trainer/{trainerId}/pending-requests")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF') or (hasRole('TRAINER') and @securityUtils.isCurrentUser(#trainerId))")
    public ResponseEntity<ApiResponse<List<TrainerBookingResponse>>> getPendingBookingRequests(
            @PathVariable Long trainerId) {
        
        log.info("Getting pending booking requests for trainer {}", trainerId);
        
        try {
            List<TrainerBookingResponse> pendingRequests = 
                trainerManagementService.getPendingBookingRequests(trainerId);
            String message = pendingRequests.isEmpty() ? 
                "Không có yêu cầu đặt lịch nào đang chờ" : 
                String.format("Có %d yêu cầu đặt lịch đang chờ xử lý", pendingRequests.size());
            
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
                    .message("Lỗi khi lấy danh sách yêu cầu: " + e.getMessage())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }

    /**
     * Get trainer statistics
     */
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
                .message("Lấy thống kê trainer thành công")
                .data(statistics)
                .status(HttpStatus.OK.value())
                .build());
                
        } catch (Exception e) {
            log.error("Error getting trainer statistics for trainer {}", trainerId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<TrainerStatisticsResponse>builder()
                    .success(false)
                    .message("Lỗi khi lấy thống kê: " + e.getMessage())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }

    /**
     * Get all trainers with their current status (for admin overview)
     */
    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<TrainerScheduleResponse>>> getTrainersOverview(
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDate targetDate = date != null ? date : LocalDate.now();
        log.info("Getting trainers overview for date {}", targetDate);
        
        try {
            List<TrainerScheduleResponse> trainersOverview = 
                trainerManagementService.getTrainersOverview(targetDate);
            
            return ResponseEntity.ok(ApiResponse.<List<TrainerScheduleResponse>>builder()
                .success(true)
                .message(String.format("Lấy tổng quan %d trainer thành công", trainersOverview.size()))
                .data(trainersOverview)
                .status(HttpStatus.OK.value())
                .build());
                
        } catch (Exception e) {
            log.error("Error getting trainers overview for date {}", targetDate, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<List<TrainerScheduleResponse>>builder()
                    .success(false)
                    .message("Lỗi khi lấy tổng quan trainer: " + e.getMessage())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }

    /**
     * Get trainer workload summary (for admin resource planning)
     */
    @GetMapping("/workload-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<TrainerStatisticsResponse>>> getWorkloadSummary(
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        
        log.info("Getting workload summary from {} to {}", fromDate, toDate);
        
        try {
            // Default to current month if no dates provided
            LocalDate startDate = fromDate != null ? fromDate : LocalDate.now().withDayOfMonth(1);
            LocalDate endDate = toDate != null ? toDate : LocalDate.now();
            
            List<TrainerStatisticsResponse> workloadSummary = 
                trainerManagementService.getWorkloadSummary(startDate, endDate);
            
            return ResponseEntity.ok(ApiResponse.<List<TrainerStatisticsResponse>>builder()
                .success(true)
                .message("Lấy tổng quan khối lượng công việc thành công")
                .data(workloadSummary)
                .status(HttpStatus.OK.value())
                .build());
                
        } catch (Exception e) {
            log.error("Error getting workload summary", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<List<TrainerStatisticsResponse>>builder()
                    .success(false)
                    .message("Lỗi khi lấy tổng quan khối lượng công việc: " + e.getMessage())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }
}