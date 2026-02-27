package com.example.project_backend04.service.IService;

import com.example.project_backend04.dto.request.Role.RoleCreateDto;
import com.example.project_backend04.dto.request.Role.RoleUpdateDto;
import com.example.project_backend04.dto.request.User.CreateUserRequest;
import com.example.project_backend04.dto.request.User.UpdateUserRequest;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.dto.response.User.UserResponse;
import com.example.project_backend04.entity.Role;

import java.util.List;


public interface IAdminService {
    ApiResponse<Role> createRole(RoleCreateDto request);
    ApiResponse<List<Role>> getAllRoles();
    ApiResponse<Role> updateRole(RoleUpdateDto dto);
    ApiResponse<Void> deleteRole(Long id);

    ApiResponse<UserResponse> createUser(CreateUserRequest req);
    ApiResponse<UserResponse> updateUser(Long id, UpdateUserRequest req);
    ApiResponse<Void> deleteUser(Long id);
    ApiResponse<List<UserResponse>> getAllUsers();

}
