package com.example.project_backend04.controller;

import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.entity.Membership;
import com.example.project_backend04.repository.MembershipRepository;
import com.example.project_backend04.repository.UserRepository;
import com.example.project_backend04.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user/memberships")
@RequiredArgsConstructor
public class UserMembershipController {

    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyMemberships(
            Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            var user = userRepository.findById(userDetails.getId()).orElseThrow();
            List<Membership> memberships = membershipRepository.findByUserOrderByCreateDateDesc(user);

            List<Map<String, Object>> data = memberships.stream().map(m -> {
                var pkg = m.getMembershipPackage();
                return Map.<String, Object>of(
                        "id", m.getId(),
                        "membershipId", m.getId().toString(),
                        "status", m.getStatus().name(),
                        "startDate", m.getStartDate().toString(),
                        "endDate", m.getEndDate().toString(),
                        "paidAmount", m.getPaidAmount(),
                        "membershipPackage", Map.of(
                                "id", pkg.getId(),
                                "name", pkg.getName() != null ? pkg.getName() : "",
                                "description", pkg.getDescription() != null ? pkg.getDescription() : "",
                                "duration", pkg.getDuration(),
                                "price", pkg.getPrice(),
                                "features", pkg.getFeatures() != null ? pkg.getFeatures() : List.of(),
                                "color", pkg.getColor() != null ? pkg.getColor() : ""
                        )
                );
            }).collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(data, "Memberships retrieved successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyActiveMemberships(
            Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            var user = userRepository.findById(userDetails.getId()).orElseThrow();
            List<Membership> memberships = membershipRepository
                    .findByUserAndStatus(user, Membership.MembershipStatus.ACTIVE)
                    .stream()
                    .filter(Membership::isActive)
                    .collect(Collectors.toList());

            List<Map<String, Object>> data = memberships.stream().map(m -> {
                var pkg = m.getMembershipPackage();
                return Map.<String, Object>of(
                        "id", m.getId(),
                        "membershipId", m.getId().toString(),
                        "status", m.getStatus().name(),
                        "startDate", m.getStartDate().toString(),
                        "endDate", m.getEndDate().toString(),
                        "paidAmount", m.getPaidAmount(),
                        "membershipPackage", Map.of(
                                "id", pkg.getId(),
                                "name", pkg.getName() != null ? pkg.getName() : "",
                                "description", pkg.getDescription() != null ? pkg.getDescription() : "",
                                "duration", pkg.getDuration(),
                                "price", pkg.getPrice(),
                                "features", pkg.getFeatures() != null ? pkg.getFeatures() : List.of(),
                                "color", pkg.getColor() != null ? pkg.getColor() : ""
                        )
                );
            }).collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(data, "Active memberships retrieved"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/active-packages")
    public ResponseEntity<ApiResponse<List<Long>>> getMyActivePackages(Authentication authentication) {
        try {
            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
            }
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getId();
            List<Long> activePackageIds = membershipRepository.findActivePackageIdsByUserId(userId, LocalDate.now());
            return ResponseEntity.ok(ApiResponse.success(activePackageIds, "Active packages retrieved successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to retrieve active packages: " + e.getMessage()));
        }
    }
}
