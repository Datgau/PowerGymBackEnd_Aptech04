package com.example.project_backend04.controller;

import com.example.project_backend04.dto.request.TrainerBooking.CreateServiceLinkedBookingRequest;
import com.example.project_backend04.dto.request.TrainerBooking.TrainerConfirmationRequest;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.dto.response.TrainerBooking.TrainerBookingResponse;
import com.example.project_backend04.service.IService.IIntegratedBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/integrated-bookings")
@RequiredArgsConstructor
@Slf4j
public class IntegratedTrainerBookingController {
    
    private final IIntegratedBookingService integratedBookingService;
    
    /**
     * Create booking linked to service registration
     */
    @PostMapping("/service-linked")
    public ResponseEntity<ApiResponse<TrainerBookingResponse>> createServiceLinkedBooking(
            @Valid @RequestBody CreateServiceLinkedBookingRequest request) {
        
        log.info("Creating service-linked booking for registration {}", request.getServiceRegistrationId());
        
        try {
            TrainerBookingResponse response = integratedBookingService.createServiceLinkedBooking(request);
            
            return ResponseEntity.ok(ApiResponse.<TrainerBookingResponse>builder()
                .success(true)
                .message("Booking created successfully and sent to trainer for confirmation")
                .data(response)
                .status(HttpStatus.OK.value())
                .build());
                
        } catch (Exception e) {
            log.error("Error creating service-linked booking", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<TrainerBookingResponse>builder()
                    .success(false)
                    .message("Failed to create booking: " + e.getMessage())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }
    
    /**
     * Get all bookings for a service registration
     */
    @GetMapping("/service-registration/{registrationId}")
    public ResponseEntity<ApiResponse<List<TrainerBookingResponse>>> getServiceBookings(
            @PathVariable Long registrationId) {
        
        log.debug("Getting bookings for service registration {}", registrationId);
        
        try {
            List<TrainerBookingResponse> bookings = integratedBookingService.getServiceBookings(registrationId);
            
            String message = bookings.isEmpty() ? 
                "No bookings found for this service registration" : 
                String.format("Found %d booking%s", bookings.size(), bookings.size() > 1 ? "s" : "");
            
            return ResponseEntity.ok(ApiResponse.<List<TrainerBookingResponse>>builder()
                .success(true)
                .message(message)
                .data(bookings)
                .status(HttpStatus.OK.value())
                .build());
                
        } catch (Exception e) {
            log.error("Error getting bookings for service registration {}", registrationId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<List<TrainerBookingResponse>>builder()
                    .success(false)
                    .message("Failed to get service bookings: " + e.getMessage())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }
    
    /**
     * Trainer confirms or rejects booking
     */
    @PutMapping("/{bookingId}/trainer-response")
    public ResponseEntity<ApiResponse<TrainerBookingResponse>> trainerResponseToBooking(
            @PathVariable Long bookingId,
            @Valid @RequestBody TrainerConfirmationRequest request) {
        
        log.info("Trainer responding to booking {} - confirmed: {}", bookingId, request.isConfirmed());
        
        try {
            TrainerBookingResponse response;
            
            if (request.isConfirmed()) {
                response = integratedBookingService.confirmBooking(bookingId, request.getTrainerNotes());
            } else {
                String rejectionReason = request.getRejectionReason() != null ? 
                    request.getRejectionReason() : "No reason provided";
                response = integratedBookingService.rejectBooking(bookingId, rejectionReason);
            }
            
            String message = request.isConfirmed() ? 
                "Booking confirmed successfully" : "Booking rejected";
            
            return ResponseEntity.ok(ApiResponse.<TrainerBookingResponse>builder()
                .success(true)
                .message(message)
                .data(response)
                .status(HttpStatus.OK.value())
                .build());
                
        } catch (Exception e) {
            log.error("Error processing trainer response for booking {}", bookingId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<TrainerBookingResponse>builder()
                    .success(false)
                    .message("Failed to process trainer response: " + e.getMessage())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }
    
    /**
     * Reschedule booking
     */
    @PutMapping("/{bookingId}/reschedule")
    public ResponseEntity<ApiResponse<TrainerBookingResponse>> rescheduleBooking(
            @PathVariable Long bookingId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime newStartTime) {
        
        log.info("Rescheduling booking {} to {} at {}", bookingId, newDate, newStartTime);
        
        try {
            TrainerBookingResponse response = integratedBookingService
                .rescheduleBooking(bookingId, newDate, newStartTime);
            
            return ResponseEntity.ok(ApiResponse.<TrainerBookingResponse>builder()
                .success(true)
                .message("Booking rescheduled successfully")
                .data(response)
                .status(HttpStatus.OK.value())
                .build());
                
        } catch (Exception e) {
            log.error("Error rescheduling booking {}", bookingId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<TrainerBookingResponse>builder()
                    .success(false)
                    .message("Failed to reschedule booking: " + e.getMessage())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }
    
    /**
     * Complete session with feedback
     */
    @PutMapping("/{bookingId}/complete")
    public ResponseEntity<ApiResponse<TrainerBookingResponse>> completeSession(
            @PathVariable Long bookingId,
            @RequestParam(required = false) String clientFeedback,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String trainerNotes) {
        
        log.info("Completing session for booking {}", bookingId);
        
        try {
            TrainerBookingResponse response = integratedBookingService
                .completeSession(bookingId, clientFeedback, rating, trainerNotes);
            
            return ResponseEntity.ok(ApiResponse.<TrainerBookingResponse>builder()
                .success(true)
                .message("Session completed successfully")
                .data(response)
                .status(HttpStatus.OK.value())
                .build());
                
        } catch (Exception e) {
            log.error("Error completing session for booking {}", bookingId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<TrainerBookingResponse>builder()
                    .success(false)
                    .message("Failed to complete session: " + e.getMessage())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }
    
    /**
     * Cancel booking
     */
    @PutMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<TrainerBookingResponse>> cancelBooking(
            @PathVariable Long bookingId,
            @RequestParam(required = false) String cancellationReason) {
        
        log.info("Cancelling booking {}", bookingId);
        
        try {
            String reason = cancellationReason != null ? cancellationReason : "No reason provided";
            TrainerBookingResponse response = integratedBookingService.cancelBooking(bookingId, reason);
            
            return ResponseEntity.ok(ApiResponse.<TrainerBookingResponse>builder()
                .success(true)
                .message("Booking cancelled successfully")
                .data(response)
                .status(HttpStatus.OK.value())
                .build());
                
        } catch (Exception e) {
            log.error("Error cancelling booking {}", bookingId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<TrainerBookingResponse>builder()
                    .success(false)
                    .message("Failed to cancel booking: " + e.getMessage())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }
    
    /**
     * Mark booking as no-show
     */
    @PutMapping("/{bookingId}/no-show")
    public ResponseEntity<ApiResponse<TrainerBookingResponse>> markNoShow(
            @PathVariable Long bookingId,
            @RequestParam(required = false) String notes) {
        
        log.info("Marking booking {} as no-show", bookingId);
        
        try {
            TrainerBookingResponse response = integratedBookingService.markNoShow(bookingId, notes);
            
            return ResponseEntity.ok(ApiResponse.<TrainerBookingResponse>builder()
                .success(true)
                .message("Booking marked as no-show")
                .data(response)
                .status(HttpStatus.OK.value())
                .build());
                
        } catch (Exception e) {
            log.error("Error marking booking {} as no-show", bookingId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<TrainerBookingResponse>builder()
                    .success(false)
                    .message("Failed to mark as no-show: " + e.getMessage())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }
    
    /**
     * Get pending bookings for trainer
     */
    @GetMapping("/trainer/{trainerId}/pending")
    public ResponseEntity<ApiResponse<List<TrainerBookingResponse>>> getPendingBookings(
            @PathVariable Long trainerId) {
        
        log.debug("Getting pending bookings for trainer {}", trainerId);
        
        try {
            List<TrainerBookingResponse> pendingBookings = integratedBookingService
                .getPendingBookings(trainerId);
            
            String message = pendingBookings.isEmpty() ? 
                "No pending bookings found" : 
                String.format("Found %d pending booking%s", 
                    pendingBookings.size(), pendingBookings.size() > 1 ? "s" : "");
            
            return ResponseEntity.ok(ApiResponse.<List<TrainerBookingResponse>>builder()
                .success(true)
                .message(message)
                .data(pendingBookings)
                .status(HttpStatus.OK.value())
                .build());
                
        } catch (Exception e) {
            log.error("Error getting pending bookings for trainer {}", trainerId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<List<TrainerBookingResponse>>builder()
                    .success(false)
                    .message("Failed to get pending bookings: " + e.getMessage())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }
    
    /**
     * Get upcoming bookings for user
     */
    @GetMapping("/user/{userId}/upcoming")
    public ResponseEntity<ApiResponse<List<TrainerBookingResponse>>> getUpcomingBookings(
            @PathVariable Long userId) {
        
        log.debug("Getting upcoming bookings for user {}", userId);
        
        try {
            List<TrainerBookingResponse> upcomingBookings = integratedBookingService
                .getUpcomingBookings(userId);
            
            String message = upcomingBookings.isEmpty() ? 
                "No upcoming bookings found" : 
                String.format("Found %d upcoming booking%s", 
                    upcomingBookings.size(), upcomingBookings.size() > 1 ? "s" : "");
            
            return ResponseEntity.ok(ApiResponse.<List<TrainerBookingResponse>>builder()
                .success(true)
                .message(message)
                .data(upcomingBookings)
                .status(HttpStatus.OK.value())
                .build());
                
        } catch (Exception e) {
            log.error("Error getting upcoming bookings for user {}", userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<List<TrainerBookingResponse>>builder()
                    .success(false)
                    .message("Failed to get upcoming bookings: " + e.getMessage())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }
    
    /**
     * Get booking history for user
     */
    @GetMapping("/user/{userId}/history")
    public ResponseEntity<ApiResponse<List<TrainerBookingResponse>>> getBookingHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.debug("Getting booking history for user {} (page {}, size {})", userId, page, size);
        
        try {
            List<TrainerBookingResponse> bookingHistory = integratedBookingService
                .getBookingHistory(userId, page, size);
            
            String message = bookingHistory.isEmpty() ? 
                "No booking history found" : 
                String.format("Retrieved %d booking%s from history", 
                    bookingHistory.size(), bookingHistory.size() > 1 ? "s" : "");
            
            return ResponseEntity.ok(ApiResponse.<List<TrainerBookingResponse>>builder()
                .success(true)
                .message(message)
                .data(bookingHistory)
                .status(HttpStatus.OK.value())
                .build());
                
        } catch (Exception e) {
            log.error("Error getting booking history for user {}", userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<List<TrainerBookingResponse>>builder()
                    .success(false)
                    .message("Failed to get booking history: " + e.getMessage())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }
}