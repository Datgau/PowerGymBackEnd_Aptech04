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
}
