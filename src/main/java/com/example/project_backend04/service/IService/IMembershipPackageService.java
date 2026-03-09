package com.example.project_backend04.service.IService;

import com.example.project_backend04.dto.request.MembershipPackage.CreateMembershipPackageDto;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.entity.MembershipPackage;

import java.util.List;

public interface IMembershipPackageService {
    ApiResponse<List<MembershipPackage>> getAllPackages();
    ApiResponse<List<MembershipPackage>> getActivePackages();
    ApiResponse<MembershipPackage> getPackageById(Long id);
    ApiResponse<MembershipPackage> createMembershipPackage(CreateMembershipPackageDto request);
    ApiResponse<MembershipPackage> updateMembershipPackage(Long id, CreateMembershipPackageDto request);
    ApiResponse<Void> deleteMembershipPackage(Long id);
}
