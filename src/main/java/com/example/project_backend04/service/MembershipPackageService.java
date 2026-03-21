package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.MembershipPackage.CreateMembershipPackageDto;
import com.example.project_backend04.dto.request.MembershipPackage.UpdateMembershipPackageDto;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.entity.MembershipPackage;
import com.example.project_backend04.mapper.MembershipPackageMapper;
import com.example.project_backend04.repository.MembershipPackageRepository;
import com.example.project_backend04.service.IService.IMembershipPackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class MembershipPackageService implements IMembershipPackageService {
    final MembershipPackageRepository membershipPackageRepository;
    final MembershipPackageMapper membershipPackageMapper;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<MembershipPackage>> getAllPackages() {
        try {
            List<MembershipPackage> packages = membershipPackageRepository.findAll();
            return ApiResponse.success(packages, "Packages retrieved successfully");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<MembershipPackage>> getActivePackages() {
        try {
            List<MembershipPackage> packages = membershipPackageRepository.findByIsActiveTrue();
            return ApiResponse.success(packages, "Active packages retrieved successfully");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ApiResponse<MembershipPackage> getPackageById(Long id) {
        Optional<MembershipPackage> packageOpt = membershipPackageRepository.findById(id);
        
        if (packageOpt.isEmpty()) {
            return ApiResponse.error("Package not found");
        }
        return ApiResponse.success(packageOpt.get(), "Package retrieved successfully");
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public ApiResponse<MembershipPackage> createMembershipPackage(CreateMembershipPackageDto request) {
        try {
            MembershipPackage membershipPackage =
                    membershipPackageMapper.toEntity(request);

            MembershipPackage savedPackage =
                    membershipPackageRepository.save(membershipPackage);

            return ApiResponse.success(
                    savedPackage,
                    "Membership package created successfully"
            );
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("Failed to create membership package: " + e.getMessage());
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public ApiResponse<MembershipPackage> updateMembershipPackage(Long id, UpdateMembershipPackageDto request) {
        try {
            Optional<MembershipPackage> existingPackageOpt = membershipPackageRepository.findById(id);
            
            if (existingPackageOpt.isEmpty()) {
                return ApiResponse.error("Package not found");
            }

            MembershipPackage existingPackage = existingPackageOpt.get();
            
            // Check if packageId is being changed and if new packageId already exists
            if (!existingPackage.getPackageId().equals(request.getPackageId()) &&
                membershipPackageRepository.existsByPackageId(request.getPackageId())) {
                return ApiResponse.error("Package ID already exists");
            }

            membershipPackageMapper.updateEntityFromDto(request, existingPackage);
            
            MembershipPackage updatedPackage = membershipPackageRepository.save(existingPackage);
            
            return ApiResponse.success(updatedPackage, "Package updated successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("Failed to update membership package: " + e.getMessage());
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public ApiResponse<Void> deleteMembershipPackage(Long id) {
        Optional<MembershipPackage> packageOpt = membershipPackageRepository.findById(id);
        
        if (packageOpt.isEmpty()) {
            return ApiResponse.error("Package not found");
        }
        
        membershipPackageRepository.deleteById(id);
        
        return ApiResponse.success(null, "Package deleted successfully");
    }
}
