package com.example.project_backend04.controller;

import com.example.project_backend04.dto.response.Shared.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/membership")
@RequiredArgsConstructor
public class MembershipController {

    /**
     * Get current user's membership information
     * GET /api/membership/current
     */
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentMembership(Authentication authentication) {
        try {
            // Mock membership data
            Map<String, Object> membership = createMembershipInfo(
                1L, "PREMIUM", "2024-01-01", "2024-12-31", "ACTIVE"
            );

            ApiResponse<Map<String, Object>> response = new ApiResponse<>(
                true, "Membership retrieved successfully", membership, 200
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ApiResponse<Map<String, Object>> response = new ApiResponse<>(
                false, "Failed to get membership: " + e.getMessage(), null, 500
            );
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get all available membership packages
     * GET /api/membership/packages
     */
    @GetMapping("/packages")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPackages() {
        try {
            List<Map<String, Object>> packages = Arrays.asList(
                createPackage("basic-monthly", "Basic Monthly", 30, 299000, 399000, 
                    Arrays.asList("Gym access", "Basic equipment", "Locker room"), false, "Perfect for beginners"),
                createPackage("premium-monthly", "Premium Monthly", 30, 599000, 799000,
                    Arrays.asList("Full gym access", "All equipment", "Group classes", "Towel service"), true, "Most popular choice"),
                createPackage("vip-monthly", "VIP Monthly", 30, 999000, 1299000,
                    Arrays.asList("Full gym access", "Personal trainer sessions", "Premium amenities", "Guest passes"), false, "Ultimate experience"),
                createPackage("basic-yearly", "Basic Yearly", 365, 2990000, 4790000,
                    Arrays.asList("Gym access", "Basic equipment", "Locker room", "2 months free"), false, "Best value for basic users"),
                createPackage("premium-yearly", "Premium Yearly", 365, 5990000, 9590000,
                    Arrays.asList("Full gym access", "All equipment", "Group classes", "Towel service", "2 months free"), true, "Best value premium"),
                createPackage("vip-yearly", "VIP Yearly", 365, 9990000, 15590000,
                    Arrays.asList("Full gym access", "Personal trainer sessions", "Premium amenities", "Guest passes", "2 months free"), false, "Ultimate yearly package")
            );

            ApiResponse<List<Map<String, Object>>> response = new ApiResponse<>(
                true, "Packages retrieved successfully", packages, 200
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ApiResponse<List<Map<String, Object>>> response = new ApiResponse<>(
                false, "Failed to get packages: " + e.getMessage(), null, 500
            );
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Register for a new membership package
     * POST /api/membership/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, String>>> registerPackage(
            @RequestBody Map<String, Object> registrationRequest,
            Authentication authentication
    ) {
        try {
            String packageId = (String) registrationRequest.get("packageId");
            String paymentMethod = (String) registrationRequest.get("paymentMethod");
            String notes = (String) registrationRequest.get("notes");
            
            // Mock registration logic
            String orderId = "order-" + UUID.randomUUID().toString().substring(0, 8);
            
            Map<String, String> result = new HashMap<>();
            result.put("orderId", orderId);

            ApiResponse<Map<String, String>> response = new ApiResponse<>(
                true, "Package registration successful", result, 200
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ApiResponse<Map<String, String>> response = new ApiResponse<>(
                false, "Failed to register package: " + e.getMessage(), null, 500
            );
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Extend current membership
     * POST /api/membership/extend
     */
    @PostMapping("/extend")
    public ResponseEntity<ApiResponse<Map<String, Object>>> extendMembership(
            @RequestBody Map<String, String> extendRequest,
            Authentication authentication
    ) {
        try {
            String packageId = extendRequest.get("packageId");
            
            // Mock extended membership
            Map<String, Object> membership = createMembershipInfo(
                1L, "PREMIUM", "2024-01-01", "2025-01-31", "ACTIVE"
            );

            ApiResponse<Map<String, Object>> response = new ApiResponse<>(
                true, "Membership extended successfully", membership, 200
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ApiResponse<Map<String, Object>> response = new ApiResponse<>(
                false, "Failed to extend membership: " + e.getMessage(), null, 500
            );
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get membership history
     * GET /api/membership/history
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMembershipHistory(Authentication authentication) {
        try {
            List<Map<String, Object>> history = Arrays.asList(
                createMembershipInfo(1L, "PREMIUM", "2024-01-01", "2024-12-31", "ACTIVE"),
                createMembershipInfo(2L, "BASIC", "2023-01-01", "2023-12-31", "EXPIRED"),
                createMembershipInfo(3L, "BASIC", "2022-06-01", "2022-11-30", "EXPIRED")
            );

            ApiResponse<List<Map<String, Object>>> response = new ApiResponse<>(
                true, "Membership history retrieved successfully", history, 200
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ApiResponse<List<Map<String, Object>>> response = new ApiResponse<>(
                false, "Failed to get membership history: " + e.getMessage(), null, 500
            );
            return ResponseEntity.status(500).body(response);
        }
    }

    // Helper methods
    private Map<String, Object> createMembershipInfo(Long id, String membershipType, String startDate, String endDate, String status) {
        Map<String, Object> membership = new HashMap<>();
        membership.put("id", id);
        membership.put("membershipType", membershipType);
        membership.put("startDate", startDate);
        membership.put("endDate", endDate);
        membership.put("status", status);
        
        // Calculate days left
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        LocalDate now = LocalDate.now();
        
        long totalDays = ChronoUnit.DAYS.between(start, end);
        long daysLeft = ChronoUnit.DAYS.between(now, end);
        
        membership.put("totalDays", totalDays);
        membership.put("daysLeft", Math.max(0, daysLeft));
        membership.put("isActive", "ACTIVE".equals(status) && daysLeft > 0);
        
        return membership;
    }

    private Map<String, Object> createPackage(String id, String name, int duration, int price, int originalPrice, List<String> features, boolean isPopular, String description) {
        Map<String, Object> packageInfo = new HashMap<>();
        packageInfo.put("id", id);
        packageInfo.put("name", name);
        packageInfo.put("duration", duration);
        packageInfo.put("price", price);
        packageInfo.put("originalPrice", originalPrice);
        packageInfo.put("features", features);
        packageInfo.put("isPopular", isPopular);
        packageInfo.put("description", description);
        return packageInfo;
    }
}