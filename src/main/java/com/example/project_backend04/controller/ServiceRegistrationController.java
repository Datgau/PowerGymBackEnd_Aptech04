package com.example.project_backend04.controller;

import com.example.project_backend04.dto.request.Service.ServiceRegistrationFilterRequest;
import com.example.project_backend04.dto.request.Service.ServiceRegistrationRequest;
import com.example.project_backend04.dto.response.Service.ServiceRegistrationResponse;
import com.example.project_backend04.dto.response.Service.ServiceRegistrationWithTrainerSelectionResponse;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.dto.response.Trainer.AvailableTrainerResponse;
import com.example.project_backend04.enums.PaymentStatus;
import com.example.project_backend04.enums.RegistrationStatus;
import com.example.project_backend04.enums.RegistrationType;
import com.example.project_backend04.service.ServiceRegistrationService;
import com.example.project_backend04.service.EnhancedServiceRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-registrations")
@RequiredArgsConstructor
@Slf4j
public class ServiceRegistrationController {

    private final ServiceRegistrationService registrationService;
    private final EnhancedServiceRegistrationService enhancedServiceRegistrationService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ServiceRegistrationResponse>> registerService(
            @RequestBody ServiceRegistrationRequest request
    ) {
        try {
            ServiceRegistrationResponse response = registrationService.registerService(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "Đăng ký service thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to register service: " + e.getMessage()));
        }
    }

    @GetMapping("/{registrationId}/trainer-selection")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ServiceRegistrationWithTrainerSelectionResponse>> getRegistrationForTrainerSelection(
            @PathVariable Long registrationId
    ) {
        try {
            ServiceRegistrationWithTrainerSelectionResponse response =
                registrationService.getRegistrationForTrainerSelection(registrationId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get trainer selection data: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> cancelRegistration(@PathVariable Long id) {
        try {
            registrationService.cancelRegistration(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Hủy đăng ký thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to cancel registration: " + e.getMessage()));
        }
    }

    @GetMapping("/my-registrations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ServiceRegistrationResponse>>> getMyRegistrations() {
        try {
            log.info("Fetching my registrations for current user");
            List<ServiceRegistrationResponse> registrations = registrationService.getMyRegistrations();
            log.info("Successfully fetched {} registrations", registrations.size());
            return ResponseEntity.ok(ApiResponse.success(registrations));
        } catch (Exception e) {
            log.error("Failed to fetch my registrations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch registrations: " + e.getMessage()));
        }
    }

    @GetMapping("/service/{serviceId}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<ServiceRegistrationResponse>>> getServiceRegistrationsPaginated(
            @PathVariable Long serviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.success(registrationService.getServiceRegistrations(serviceId, page, size)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch registrations: " + e.getMessage()));
        }
    }

    @GetMapping("/service/{serviceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<ServiceRegistrationResponse>>> getServiceRegistrations(
            @PathVariable Long serviceId
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.success(registrationService.getServiceRegistrations(serviceId)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch registrations: " + e.getMessage()));
        }
    }

    @GetMapping("/all/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<ServiceRegistrationResponse>>> getAllRegistrationsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) RegistrationStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) RegistrationType registrationType,
            @RequestParam(required = false) String searchQuery
    ) {
        try {
            ServiceRegistrationFilterRequest request = new ServiceRegistrationFilterRequest();
            request.setPage(page);
            request.setSize(size);
            request.setStatus(status);
            request.setPaymentStatus(paymentStatus);
            request.setRegistrationType(registrationType);
            request.setSearchQuery(searchQuery);
            
            return ResponseEntity.ok(ApiResponse.success(registrationService.getAllRegistrationsWithFilters(request)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch registrations: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/available-trainers")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<AvailableTrainerResponse>>> getAvailableTrainers(
            @PathVariable Long id
    ) {
        try {
            List<AvailableTrainerResponse> trainers = registrationService.getAvailableTrainersForRegistration(id);
            return ResponseEntity.ok(ApiResponse.success(trainers));
        } catch (RuntimeException e) {
            if (e.getMessage().equals("Registration not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch available trainers: " + e.getMessage()));
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<ServiceRegistrationResponse>>> getAllRegistrations() {
        try {
            return ResponseEntity.ok(ApiResponse.success(registrationService.getAllRegistrations()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch registrations: " + e.getMessage()));
        }
    }

    @PutMapping("/{registrationId}/assign-trainer")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Void>> assignTrainer(
            @PathVariable Long registrationId,
            @RequestParam Long trainerId,
            @RequestParam(required = false) String notes
    ) {
        try {
            enhancedServiceRegistrationService.assignTrainer(registrationId, trainerId, notes);
            return ResponseEntity.ok(ApiResponse.success(null, "Trainer assigned successfully"));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to assign trainer: " + e.getMessage()));
        }
    }

    @PostMapping("/{registrationId}/confirm-payment")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Void>> confirmCounterPayment(
            @PathVariable Long registrationId,
            @RequestParam Long amount
    ) {
        try {
            enhancedServiceRegistrationService.confirmCounterPayment(registrationId, amount);
            return ResponseEntity.ok(ApiResponse.success(null, "Payment confirmed successfully"));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to confirm payment: " + e.getMessage()));
        }
    }
}
