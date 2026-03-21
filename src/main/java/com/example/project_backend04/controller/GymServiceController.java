package com.example.project_backend04.controller;
import com.example.project_backend04.dto.request.Service.GymServiceRequest;
import com.example.project_backend04.dto.request.Service.UpdateGymServiceDto;
import com.example.project_backend04.dto.response.Service.GymServiceResponse;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.service.IService.IGymService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gym/services")
@RequiredArgsConstructor
public class GymServiceController {

    private final IGymService gymService;

    // ===================== PUBLIC =====================

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<GymServiceResponse>>> getActiveServices() {
        try {
            List<GymServiceResponse> services = gymService.getPublicServices();
            return ResponseEntity.ok(ApiResponse.success(services));
        } catch (Exception e) {
            e.printStackTrace(); // Log the full stack trace for debugging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch services: " + e.getMessage()));
        }
    }

    @GetMapping("/active/paginated")
    public ResponseEntity<ApiResponse<Page<GymServiceResponse>>> getActiveServicesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size
    ) {
        try {
            Page<GymServiceResponse> services = gymService.getPublicServices(page, size);
            return ResponseEntity.ok(ApiResponse.success(services));
        } catch (Exception e) {
            e.printStackTrace(); // Log the full stack trace for debugging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch services: " + e.getMessage()));
        }
    }


    @GetMapping
    public ResponseEntity<ApiResponse<List<GymServiceResponse>>> getAllServices() {
        try {
            List<GymServiceResponse> services = gymService.getAllServices();
            return ResponseEntity.ok(ApiResponse.success(services));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch services: " + e.getMessage()));
        }
    }

    @GetMapping("/paginated")
    public ResponseEntity<ApiResponse<Page<GymServiceResponse>>> getAllServicesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Page<GymServiceResponse> services = gymService.getAllServices(page, size);
            return ResponseEntity.ok(ApiResponse.success(services));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch services: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GymServiceResponse>> getServiceById(@PathVariable Long id) {
        try {
            GymServiceResponse service = gymService.getServiceById(id);
            return ResponseEntity.ok(ApiResponse.success(service));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch service: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch service: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/registration-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getServiceRegistrationStats(@PathVariable Long id) {
        try {
            Map<String, Object> stats = gymService.getServiceRegistrationStats(id);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch registration stats: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch registration stats: " + e.getMessage()));
        }
    }

    // ===================== CREATE =====================

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<GymServiceResponse>> createService(
            @ModelAttribute GymServiceRequest request
    ) {
        try {
            GymServiceResponse response = gymService.createService(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "Service created successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Validation error: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create service: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<GymServiceResponse>> updateService(
            @PathVariable Long id,
            @ModelAttribute UpdateGymServiceDto request
    ) {
        GymServiceResponse response = gymService.updateService(id, request);
        return ResponseEntity.ok(
                ApiResponse.success(response, "Service updated successfully")
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteService(
            @PathVariable Long id
    ) {
        try {
            gymService.deleteService(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Service deleted successfully"));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage()));
            }
            if (e.getMessage().contains("đang có") && e.getMessage().contains("người đăng ký")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete service: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete service: " + e.getMessage()));
        }
    }
}
