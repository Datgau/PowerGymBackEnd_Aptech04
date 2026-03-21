package com.example.project_backend04.controller;

import com.example.project_backend04.dto.request.Service.ServiceRegistrationWithTrainerRequest;
import com.example.project_backend04.dto.response.Service.ServiceRegistrationWithTrainerResponse;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.dto.response.Trainer.TrainerAvailabilityDTO;
import com.example.project_backend04.service.EnhancedServiceRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/service-registrations")
@RequiredArgsConstructor
@Slf4j
public class EnhancedServiceRegistrationController {
    
    private final EnhancedServiceRegistrationService enhancedServiceRegistrationService;


    @PostMapping("/with-trainer")
    public ResponseEntity<ApiResponse<ServiceRegistrationWithTrainerResponse>> registerServiceWithTrainer(
            @Valid @RequestBody ServiceRegistrationWithTrainerRequest request) {
        
        log.info("Received service registration with trainer request for user {} and service {}", 
                request.getUserId(), request.getServiceId());
        
        try {
            ServiceRegistrationWithTrainerResponse response = 
                enhancedServiceRegistrationService.registerServiceWithTrainer(request);
            
            return ResponseEntity.ok(ApiResponse.<ServiceRegistrationWithTrainerResponse>builder()
                .success(true)
                .message("Service registered successfully" + 
                        (response.isHasTrainer() ? " with trainer assigned" : ""))
                .data(response)
                .status(HttpStatus.OK.value())
                .build());
                
        } catch (Exception e) {
            log.error("Error registering service with trainer", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<ServiceRegistrationWithTrainerResponse>builder()
                    .success(false)
                    .message("Failed to register service: " + e.getMessage())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }
    
    /**
     * Get available trainers for a service
     */
    @GetMapping("/service/{serviceId}/available-trainers")
    public ResponseEntity<ApiResponse<List<TrainerAvailabilityDTO>>> getAvailableTrainers(
            @PathVariable Long serviceId,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate preferredDate) {
        
        log.info("Getting available trainers for service {} on date {}", serviceId, preferredDate);
        
        try {
            // Use current date if no preferred date provided
            LocalDate searchDate = preferredDate != null ? preferredDate : LocalDate.now();
            
            List<TrainerAvailabilityDTO> trainers = 
                enhancedServiceRegistrationService.getAvailableTrainers(serviceId, searchDate);
            
            String message = trainers.isEmpty() ? 
                "No available trainers found for this service" : 
                String.format("Found %d available trainer%s", trainers.size(), trainers.size() > 1 ? "s" : "");
            
            return ResponseEntity.ok(ApiResponse.<List<TrainerAvailabilityDTO>>builder()
                .success(true)
                .message(message)
                .data(trainers)
                .status(HttpStatus.OK.value())
                .build());
                
        } catch (Exception e) {
            log.error("Error getting available trainers for service {}", serviceId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<List<TrainerAvailabilityDTO>>builder()
                    .success(false)
                    .message("Failed to get available trainers: " + e.getMessage())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }
    
    /**
     * Assign trainer to existing registration
     */
    @PutMapping("/{registrationId}/assign-trainer")
    public ResponseEntity<ApiResponse<ServiceRegistrationWithTrainerResponse>> assignTrainer(
            @PathVariable Long registrationId,
            @RequestParam Long trainerId,
            @RequestParam(required = false) String notes) {
        
        log.info("Assigning trainer {} to registration {}", trainerId, registrationId);
        
        try {
            ServiceRegistrationWithTrainerResponse response = 
                enhancedServiceRegistrationService.assignTrainer(registrationId, trainerId, notes);
            
            return ResponseEntity.ok(ApiResponse.<ServiceRegistrationWithTrainerResponse>builder()
                .success(true)
                .message("Trainer assigned successfully")
                .data(response)
                .status(HttpStatus.OK.value())
                .build());
                
        } catch (Exception e) {
            log.error("Error assigning trainer {} to registration {}", trainerId, registrationId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<ServiceRegistrationWithTrainerResponse>builder()
                    .success(false)
                    .message("Failed to assign trainer: " + e.getMessage())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }
    
    /**
     * Remove trainer from registration
     */
    @DeleteMapping("/{registrationId}/remove-trainer")
    public ResponseEntity<ApiResponse<Void>> removeTrainer(
            @PathVariable Long registrationId,
            @RequestParam(required = false) String reason) {
        
        log.info("Removing trainer from registration {}", registrationId);
        
        try {
            String removalReason = reason != null ? reason : "Trainer removed by user";
            enhancedServiceRegistrationService.removeTrainerFromRegistration(registrationId, removalReason);
            
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Trainer removed successfully")
                .status(HttpStatus.OK.value())
                .build());
                
        } catch (Exception e) {
            log.error("Error removing trainer from registration {}", registrationId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to remove trainer: " + e.getMessage())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }
    
    /**
     * Get registration with trainer and booking details
     */
    @GetMapping("/{registrationId}/full-details")
    public ResponseEntity<ApiResponse<ServiceRegistrationWithTrainerResponse>> getRegistrationFullDetails(
            @PathVariable Long registrationId) {
        
        log.debug("Getting full details for registration {}", registrationId);
        
        try {
            ServiceRegistrationWithTrainerResponse response = 
                enhancedServiceRegistrationService.getRegistrationFullDetails(registrationId);
            
            return ResponseEntity.ok(ApiResponse.<ServiceRegistrationWithTrainerResponse>builder()
                .success(true)
                .message("Registration details retrieved successfully")
                .data(response)
                .status(HttpStatus.OK.value())
                .build());
                
        } catch (Exception e) {
            log.error("Error getting registration details for {}", registrationId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<ServiceRegistrationWithTrainerResponse>builder()
                    .success(false)
                    .message("Failed to get registration details: " + e.getMessage())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }
    
    /**
     * Get user's registrations with trainer information
     */
    @GetMapping("/user/{userId}/with-trainers")
    public ResponseEntity<ApiResponse<List<ServiceRegistrationWithTrainerResponse>>> getUserRegistrationsWithTrainers(
            @PathVariable Long userId) {
        
        log.debug("Getting registrations with trainers for user {}", userId);
        
        try {
            List<ServiceRegistrationWithTrainerResponse> registrations = 
                enhancedServiceRegistrationService.getUserRegistrationsWithTrainers(userId);
            
            String message = registrations.isEmpty() ? 
                "No active registrations found" : 
                String.format("Found %d active registration%s", 
                    registrations.size(), registrations.size() > 1 ? "s" : "");
            
            return ResponseEntity.ok(ApiResponse.<List<ServiceRegistrationWithTrainerResponse>>builder()
                .success(true)
                .message(message)
                .data(registrations)
                .status(HttpStatus.OK.value())
                .build());
                
        } catch (Exception e) {
            log.error("Error getting registrations for user {}", userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<List<ServiceRegistrationWithTrainerResponse>>builder()
                    .success(false)
                    .message("Failed to get user registrations: " + e.getMessage())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }
    
    /**
     * Get trainer's registrations
     */
    @GetMapping("/trainer/{trainerId}")
    public ResponseEntity<ApiResponse<List<ServiceRegistrationWithTrainerResponse>>> getTrainerRegistrations(
            @PathVariable Long trainerId) {
        
        log.debug("Getting registrations for trainer {}", trainerId);
        
        try {
            List<ServiceRegistrationWithTrainerResponse> registrations = 
                enhancedServiceRegistrationService.getTrainerRegistrations(trainerId);
            
            String message = registrations.isEmpty() ? 
                "No active registrations found for this trainer" : 
                String.format("Found %d active registration%s for this trainer", 
                    registrations.size(), registrations.size() > 1 ? "s" : "");
            
            return ResponseEntity.ok(ApiResponse.<List<ServiceRegistrationWithTrainerResponse>>builder()
                .success(true)
                .message(message)
                .data(registrations)
                .status(HttpStatus.OK.value())
                .build());
                
        } catch (Exception e) {
            log.error("Error getting registrations for trainer {}", trainerId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<List<ServiceRegistrationWithTrainerResponse>>builder()
                    .success(false)
                    .message("Failed to get trainer registrations: " + e.getMessage())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }
    
    /**
     * Update trainer selection notes
     */
    @PutMapping("/{registrationId}/trainer-notes")
    public ResponseEntity<ApiResponse<ServiceRegistrationWithTrainerResponse>> updateTrainerNotes(
            @PathVariable Long registrationId,
            @RequestParam String notes) {
        
        log.info("Updating trainer notes for registration {}", registrationId);
        
        try {
            ServiceRegistrationWithTrainerResponse response = 
                enhancedServiceRegistrationService.updateTrainerNotes(registrationId, notes);
            
            return ResponseEntity.ok(ApiResponse.<ServiceRegistrationWithTrainerResponse>builder()
                .success(true)
                .message("Trainer notes updated successfully")
                .data(response)
                .status(HttpStatus.OK.value())
                .build());
                
        } catch (Exception e) {
            log.error("Error updating trainer notes for registration {}", registrationId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<ServiceRegistrationWithTrainerResponse>builder()
                    .success(false)
                    .message("Failed to update trainer notes: " + e.getMessage())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }
}