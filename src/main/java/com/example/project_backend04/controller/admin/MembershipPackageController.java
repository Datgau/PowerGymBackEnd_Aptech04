package com.example.project_backend04.controller.admin;

import com.example.project_backend04.dto.request.MembershipPackage.CreateMembershipPackageDto;
import com.example.project_backend04.dto.request.MembershipPackage.UpdateMembershipPackageDto;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.entity.MembershipPackage;
import com.example.project_backend04.service.IService.IMembershipPackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/membership-packages")
@RequiredArgsConstructor
public class MembershipPackageController {
    final IMembershipPackageService membershipPackageService;
    final com.example.project_backend04.repository.MembershipRepository membershipRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MembershipPackage>>> getAllPackages() {
        try {
            ApiResponse<List<MembershipPackage>> response =
                    membershipPackageService.getAllPackages();
            return ResponseEntity.status(response.getStatus()).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            ApiResponse<List<MembershipPackage>> errorResponse = 
                ApiResponse.error("Internal server error: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<MembershipPackage>>> getActivePackages() {
        try {
            ApiResponse<List<MembershipPackage>> response =
                    membershipPackageService.getActivePackages();
            return ResponseEntity.status(response.getStatus()).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            
            ApiResponse<List<MembershipPackage>> errorResponse = 
                ApiResponse.error("Internal server error: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MembershipPackage>> getPackageById(@PathVariable Long id) {
        ApiResponse<MembershipPackage> response =
                membershipPackageService.getPackageById(id);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<MembershipPackage>> createMembershipPackage(
            @Valid @RequestBody CreateMembershipPackageDto request
    ) {
        ApiResponse<MembershipPackage> response =
                membershipPackageService.createMembershipPackage(request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MembershipPackage>> updateMembershipPackage(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMembershipPackageDto request
    ) {
        ApiResponse<MembershipPackage> response =
                membershipPackageService.updateMembershipPackage(id, request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMembershipPackage(@PathVariable Long id) {
        ApiResponse<Void> response =
                membershipPackageService.deleteMembershipPackage(id);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
    
    @GetMapping("/{id}/members")
    public ResponseEntity<ApiResponse<List<com.example.project_backend04.dto.response.Membership.PackageMemberDto>>> getPackageMembers(@PathVariable Long id) {
        try {
            List<com.example.project_backend04.entity.Membership> memberships = membershipRepository.findByMembershipPackageId(id);
            
            // Convert to DTO
            List<com.example.project_backend04.dto.response.Membership.PackageMemberDto> memberDtos = memberships.stream()
                .map(membership -> com.example.project_backend04.dto.response.Membership.PackageMemberDto.builder()
                    .id(membership.getId())
                    .user(com.example.project_backend04.dto.response.Membership.MembershipUserDto.builder()
                        .id(membership.getUser().getId())
                        .fullName(membership.getUser().getFullName())
                        .email(membership.getUser().getEmail())
                        .phoneNumber(membership.getUser().getPhoneNumber())
                        .avatar(membership.getUser().getAvatar())
                        .build())
                    .startDate(membership.getStartDate())
                    .endDate(membership.getEndDate())
                    .paidAmount(membership.getPaidAmount())
                    .status(membership.getStatus().name())
                    .build())
                .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(memberDtos, "Members retrieved successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to retrieve members: " + e.getMessage()));
        }
    }
    
    @GetMapping("/user/{userId}/active-packages")
    public ResponseEntity<ApiResponse<List<Long>>> getUserActivePackages(@PathVariable Long userId) {
        try {
            List<Long> activePackageIds = membershipRepository.findActivePackageIdsByUserId(userId, java.time.LocalDate.now());
            return ResponseEntity.ok(ApiResponse.success(activePackageIds, "Active packages retrieved successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to retrieve active packages: " + e.getMessage()));
        }
    }
}
