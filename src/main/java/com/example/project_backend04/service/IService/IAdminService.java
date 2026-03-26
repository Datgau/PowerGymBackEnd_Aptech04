package com.example.project_backend04.service.IService;

import com.example.project_backend04.dto.request.Role.RoleCreateDto;
import com.example.project_backend04.dto.request.Role.RoleUpdateDto;
import com.example.project_backend04.dto.request.User.CreateUserRequest;
import com.example.project_backend04.dto.request.User.UpdateUserRequest;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.dto.response.User.UserResponse;
import com.example.project_backend04.dto.response.User.UserDetailResponse;
import com.example.project_backend04.dto.response.User.UserMembershipResponse;
import com.example.project_backend04.dto.response.User.UserServiceRegistrationResponse;
import com.example.project_backend04.dto.response.User.TrainerSpecialtyResponse;
import com.example.project_backend04.entity.Role;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;


public interface IAdminService {
    ApiResponse<Role> createRole(RoleCreateDto request);
    ApiResponse<List<Role>> getAllRoles();
    ApiResponse<Page<Role>> getAllRoles(int page, int size);
    ApiResponse<Role> updateRole(RoleUpdateDto dto);
    ApiResponse<Void> deleteRole(Long id);

    ApiResponse<UserResponse> createUser(CreateUserRequest req);
    ApiResponse<UserResponse> updateUser(Long id, UpdateUserRequest req);
    ApiResponse<Void> deleteUser(Long id);
    ApiResponse<Page<UserResponse>> getAllUsers(int page, int size);
    ApiResponse<Page<UserResponse>> getUsersByRole(String roleName, int page, int size);
    ApiResponse<Map<String, Long>> getUserCounts();
    ApiResponse<Page<UserResponse>> searchUsers(String searchTerm, String role, int page, int size);
    
    // User detail methods
    ApiResponse<UserDetailResponse> getUserDetail(Long id);
    ApiResponse<List<UserMembershipResponse>> getUserMemberships(Long userId);
    ApiResponse<List<UserServiceRegistrationResponse>> getUserServiceRegistrations(Long userId);
    ApiResponse<List<TrainerSpecialtyResponse>> getTrainerSpecialties(Long userId);
    
    // User status management
    ApiResponse<UserResponse> toggleUserStatus(Long userId);
}
