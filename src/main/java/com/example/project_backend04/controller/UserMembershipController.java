package com.example.project_backend04.controller;

import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.repository.MembershipRepository;
import com.example.project_backend04.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/user/memberships")
@RequiredArgsConstructor
public class UserMembershipController {
    
    private final MembershipRepository membershipRepository;
    
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
