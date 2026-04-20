package com.example.project_backend04.controller;

import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.entity.ServiceRegistration;
import com.example.project_backend04.enums.RegistrationStatus;
import com.example.project_backend04.repository.ServiceRegistrationRepository;
import com.example.project_backend04.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user/service-registrations")
@RequiredArgsConstructor
public class UserServiceRegistrationController {

    private final ServiceRegistrationRepository serviceRegistrationRepository;

    /** GET /api/user/service-registrations — tất cả registrations */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyRegistrations(
            Authentication authentication) {
        try {
            Long userId = uid(authentication);
            var list = serviceRegistrationRepository
                    .findByUserIdAndStatusWithBookings(userId, RegistrationStatus.ACTIVE);
            return ResponseEntity.ok(ApiResponse.success(toMaps(list), "OK"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/active")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyActiveRegistrations(
            Authentication authentication) {
        try {
            Long userId = uid(authentication);
            var list = serviceRegistrationRepository
                    .findByUserIdAndStatusWithBookings(userId, RegistrationStatus.ACTIVE)
                    .stream()
                    .filter(sr -> !sr.isExpired())
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(toMaps(list), "OK"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
        }
    }


    private Long uid(Authentication auth) {
        return ((CustomUserDetails) auth.getPrincipal()).getId();
    }

    private List<Map<String, Object>> toMaps(List<ServiceRegistration> list) {
        return list.stream().map(sr -> {
            var svc = sr.getGymService();
            var trainer = sr.getTrainer();

            // Lấy bookings CONFIRMED hoặc COMPLETED
            var bookings = sr.getTrainerBookings() == null ? List.of() :
                    sr.getTrainerBookings().stream()
                            .filter(b -> b.getStatus() != null &&
                                    (b.getStatus().name().equals("CONFIRMED") ||
                                     b.getStatus().name().equals("COMPLETED")))
                            .map(b -> Map.<String, Object>of(
                                    "id", b.getId(),
                                    "bookingDate", b.getBookingDate() != null
                                            ? b.getBookingDate().toString() : "",
                                    "startTime", b.getStartTime() != null
                                            ? b.getStartTime().toString() : "",
                                    "endTime", b.getEndTime() != null
                                            ? b.getEndTime().toString() : "",
                                    "status", b.getStatus().name()
                            ))
                            .collect(Collectors.toList());

            return Map.<String, Object>of(
                    "id", sr.getId(),
                    "status", sr.getStatus().name(),
                    "registrationDate", sr.getRegistrationDate() != null
                            ? sr.getRegistrationDate().toString() : "",
                    "expirationDate", sr.getExpirationDate() != null
                            ? sr.getExpirationDate().toString() : "",
                    "gymService", Map.of(
                            "id", svc.getId(),
                            "name", svc.getName() != null ? svc.getName() : "",
                            "description", svc.getDescription() != null ? svc.getDescription() : "",
                            "duration", svc.getDuration() != null ? svc.getDuration() : 0,
                            "price", svc.getPrice() != null ? svc.getPrice() : 0
                    ),
                    "trainer", trainer != null ? Map.of(
                            "id", trainer.getId(),
                            "fullName", trainer.getFullName() != null ? trainer.getFullName() : "",
                            "avatar", trainer.getAvatar() != null ? trainer.getAvatar() : ""
                    ) : Map.of(),
                    "bookings", bookings
            );
        }).collect(Collectors.toList());
    }
}
