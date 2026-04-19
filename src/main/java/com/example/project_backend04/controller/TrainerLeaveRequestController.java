package com.example.project_backend04.controller;

import com.example.project_backend04.dto.request.TrainerLeave.LeaveRequestCreateRequest;
import com.example.project_backend04.dto.request.TrainerLeave.LeaveRequestReviewRequest;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.dto.response.TrainerLeave.LeaveRequestResponse;
import com.example.project_backend04.service.TrainerLeaveRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trainer-leave-requests")
@RequiredArgsConstructor
@Slf4j
public class TrainerLeaveRequestController {
    
    private final TrainerLeaveRequestService leaveRequestService;
    
    /**
     * Create a new leave request (Trainer only)
     */
    @PostMapping("/trainer/{trainerId}")
    @PreAuthorize("hasRole('TRAINER') and @securityUtils.isCurrentUser(#trainerId)")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> createLeaveRequest(
            @PathVariable Long trainerId,
            @Valid @RequestBody LeaveRequestCreateRequest request) {
        
        log.info("Creating leave request for trainer {}", trainerId);
        
        try {
            LeaveRequestResponse response = leaveRequestService.createLeaveRequest(trainerId, request);
            
            return ResponseEntity.ok(ApiResponse.<LeaveRequestResponse>builder()
                .success(true)
                .message("Yêu cầu nghỉ đã được tạo thành công")
                .data(response)
                .status(HttpStatus.OK.value())
                .build());
                
        } catch (Exception e) {
            log.error("Error creating leave request for trainer {}", trainerId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<LeaveRequestResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }
    
    /**
     * Get all leave requests for a trainer
     */
    @GetMapping("/trainer/{trainerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF') or (hasRole('TRAINER') and @securityUtils.isCurrentUser(#trainerId))")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> getTrainerLeaveRequests(
            @PathVariable Long trainerId) {
        
        log.info("Getting leave requests for trainer {}", trainerId);
        
        try {
            List<LeaveRequestResponse> requests = leaveRequestService.getTrainerLeaveRequests(trainerId);
            
            return ResponseEntity.ok(ApiResponse.<List<LeaveRequestResponse>>builder()
                .success(true)
                .message("Lấy danh sách yêu cầu nghỉ thành công")
                .data(requests)
                .status(HttpStatus.OK.value())
                .build());
                
        } catch (Exception e) {
            log.error("Error getting leave requests for trainer {}", trainerId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<List<LeaveRequestResponse>>builder()
                    .success(false)
                    .message(e.getMessage())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }
    
    /**
     * Get all pending leave requests (Admin only)
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> getAllPendingLeaveRequests() {
        
        log.info("Getting all pending leave requests");
        
        try {
            List<LeaveRequestResponse> requests = leaveRequestService.getAllPendingLeaveRequests();
            
            return ResponseEntity.ok(ApiResponse.<List<LeaveRequestResponse>>builder()
                .success(true)
                .message(String.format("Có %d yêu cầu nghỉ đang chờ duyệt", requests.size()))
                .data(requests)
                .status(HttpStatus.OK.value())
                .build());
                
        } catch (Exception e) {
            log.error("Error getting pending leave requests", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<List<LeaveRequestResponse>>builder()
                    .success(false)
                    .message(e.getMessage())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }
    
    /**
     * Review (approve/reject) a leave request (Admin only)
     */
    @PutMapping("/{requestId}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> reviewLeaveRequest(
            @PathVariable Long requestId,
            @RequestParam Long adminId,
            @Valid @RequestBody LeaveRequestReviewRequest request) {
        
        log.info("Reviewing leave request {} by admin {}", requestId, adminId);
        
        try {
            LeaveRequestResponse response = leaveRequestService.reviewLeaveRequest(requestId, adminId, request);
            
            String message = request.getStatus().name().equals("APPROVED") 
                ? "Đã duyệt yêu cầu nghỉ" 
                : "Đã từ chối yêu cầu nghỉ";
            
            return ResponseEntity.ok(ApiResponse.<LeaveRequestResponse>builder()
                .success(true)
                .message(message)
                .data(response)
                .status(HttpStatus.OK.value())
                .build());
                
        } catch (Exception e) {
            log.error("Error reviewing leave request {}", requestId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<LeaveRequestResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }
    
    /**
     * Delete a leave request (Trainer only, pending requests only)
     */
    @DeleteMapping("/{requestId}/trainer/{trainerId}")
    @PreAuthorize("hasRole('TRAINER') and @securityUtils.isCurrentUser(#trainerId)")
    public ResponseEntity<ApiResponse<Void>> deleteLeaveRequest(
            @PathVariable Long requestId,
            @PathVariable Long trainerId) {
        
        log.info("Deleting leave request {} by trainer {}", requestId, trainerId);
        
        try {
            leaveRequestService.deleteLeaveRequest(requestId, trainerId);
            
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Đã xóa yêu cầu nghỉ")
                .status(HttpStatus.OK.value())
                .build());
                
        } catch (Exception e) {
            log.error("Error deleting leave request {}", requestId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message(e.getMessage())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }
}
