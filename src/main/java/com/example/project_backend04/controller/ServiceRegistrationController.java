package com.example.project_backend04.controller;

import com.example.project_backend04.dto.request.Service.ServiceRegistrationRequest;
import com.example.project_backend04.dto.response.Service.ServiceRegistrationResponse;
import com.example.project_backend04.dto.response.Service.ServiceRegistrationWithTrainerSelectionResponse;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.service.ServiceRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-registrations")
@RequiredArgsConstructor
public class ServiceRegistrationController {

    private final ServiceRegistrationService registrationService;
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to register service: " + e.getMessage()));
        }
    }

    /**
     * Get registration details with trainer selection options
     * This endpoint is called after successful service registration or payment
     */
    @GetMapping("/{registrationId}/trainer-selection")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ServiceRegistrationWithTrainerSelectionResponse>> getRegistrationForTrainerSelection(
            @PathVariable Long registrationId
    ) {
        try {
            ServiceRegistrationWithTrainerSelectionResponse response = 
                registrationService.getRegistrationForTrainerSelection(registrationId);
            return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin đăng ký và danh sách trainer thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to cancel registration: " + e.getMessage()));
        }
    }

    @GetMapping("/my-registrations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ServiceRegistrationResponse>>> getMyRegistrations() {
        try {
            List<ServiceRegistrationResponse> registrations = registrationService.getMyRegistrations();
            return ResponseEntity.ok(ApiResponse.success(registrations));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch registrations: " + e.getMessage()));
        }
    }

    // ===================== ADMIN APIs =====================

    @GetMapping("/service/{serviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ServiceRegistrationResponse>>> getServiceRegistrations(
            @PathVariable Long serviceId
    ) {
        try {
            List<ServiceRegistrationResponse> registrations = registrationService.getServiceRegistrations(serviceId);
            return ResponseEntity.ok(ApiResponse.success(registrations));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch registrations: " + e.getMessage()));
        }
    }

    @GetMapping("/service/{serviceId}/paginated")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<ServiceRegistrationResponse>>> getServiceRegistrationsPaginated(
            @PathVariable Long serviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Page<ServiceRegistrationResponse> registrations = registrationService.getServiceRegistrations(serviceId, page, size);
            return ResponseEntity.ok(ApiResponse.success(registrations));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch registrations: " + e.getMessage()));
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ServiceRegistrationResponse>>> getAllRegistrations() {
        try {
            List<ServiceRegistrationResponse> registrations = registrationService.getAllRegistrations();
            return ResponseEntity.ok(ApiResponse.success(registrations));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch registrations: " + e.getMessage()));
        }
    }

    @GetMapping("/all/paginated")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<ServiceRegistrationResponse>>> getAllRegistrationsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Page<ServiceRegistrationResponse> registrations = registrationService.getAllRegistrations(page, size);
            return ResponseEntity.ok(ApiResponse.success(registrations));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch registrations: " + e.getMessage()));
        }
    }
}
