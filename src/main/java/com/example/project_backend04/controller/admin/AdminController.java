package com.example.project_backend04.controller.admin;

import com.example.project_backend04.dto.request.Role.RoleCreateDto;
import com.example.project_backend04.dto.request.Role.RoleUpdateDto;
import com.example.project_backend04.dto.request.User.CreateUserRequest;
import com.example.project_backend04.dto.request.User.UpdateUserRequest;
import com.example.project_backend04.dto.response.User.UserResponse;
import com.example.project_backend04.dto.response.User.UserDetailResponse;
import com.example.project_backend04.dto.response.User.UserMembershipResponse;
import com.example.project_backend04.dto.response.User.UserServiceRegistrationResponse;
import com.example.project_backend04.dto.response.User.TrainerSpecialtyResponse;
import com.example.project_backend04.entity.Role;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.service.IService.IAdminService;
import com.example.project_backend04.service.IService.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AdminController {

    private final IAdminService adminService;
    private final IUserService userService;

    @PostMapping("/role")
    public ResponseEntity<ApiResponse<Role>> createRole(@Valid @RequestBody RoleCreateDto request) {
        ApiResponse<Role> response = adminService.createRole(request);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<Role>>> getAllRoles() {
        ApiResponse<List<Role>> response = adminService.getAllRoles();
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @GetMapping("/roles/paginated")
    public ResponseEntity<ApiResponse<Page<Role>>> getAllRolesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        ApiResponse<Page<Role>> response = adminService.getAllRoles(page, size);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }
    @PutMapping("/role/{id}")
    public ResponseEntity<ApiResponse<Role>> updateRole(@Valid @RequestBody RoleUpdateDto dto) {
        ApiResponse<Role> response = adminService.updateRole(dto);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }
    @DeleteMapping("/role/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) {
        ApiResponse<Void> response = adminService.deleteRole(id);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    // ---------------- USER ----------------
    @PostMapping("/user")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        ApiResponse<UserResponse> response = adminService.createUser(request);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @PutMapping("/user/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        ApiResponse<UserResponse> response = adminService.updateUser(id, request);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @DeleteMapping("/user/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        ApiResponse<Void> response = adminService.deleteUser(id);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        ApiResponse<Page<UserResponse>> response =
                adminService.getAllUsers(page, size);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @GetMapping("/users/by-role")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsersByRole(
            @RequestParam String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        ApiResponse<Page<UserResponse>> response =
                adminService.getUsersByRole(role, page, size);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @GetMapping("/users/counts")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUserCounts() {
        ApiResponse<Map<String, Long>> response = adminService.getUserCounts();
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @GetMapping("/users/search")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> searchUsers(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "ALL") String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        ApiResponse<Page<UserResponse>> response = 
                adminService.searchUsers(searchTerm, role, page, size);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    // User detail endpoints
    @GetMapping("/user/{id}/details")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getUserDetail(@PathVariable Long id) {
        ApiResponse<UserDetailResponse> response = adminService.getUserDetail(id);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.NOT_FOUND)
                .body(response);
    }

    @GetMapping("/user/{userId}/memberships")
    public ResponseEntity<ApiResponse<List<UserMembershipResponse>>> getUserMemberships(@PathVariable Long userId) {
        ApiResponse<List<UserMembershipResponse>> response = adminService.getUserMemberships(userId);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.NOT_FOUND)
                .body(response);
    }

    @GetMapping("/user/{userId}/service-registrations")
    public ResponseEntity<ApiResponse<List<UserServiceRegistrationResponse>>> getUserServiceRegistrations(@PathVariable Long userId) {
        ApiResponse<List<UserServiceRegistrationResponse>> response = adminService.getUserServiceRegistrations(userId);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.NOT_FOUND)
                .body(response);
    }

    @GetMapping("/user/{userId}/trainer-specialties")
    public ResponseEntity<ApiResponse<List<TrainerSpecialtyResponse>>> getTrainerSpecialties(@PathVariable Long userId) {
        ApiResponse<List<TrainerSpecialtyResponse>> response = adminService.getTrainerSpecialties(userId);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @GetMapping("/user/exists")
    public ResponseEntity<ApiResponse<Boolean>> checkUserExistsByEmail(
            @RequestParam String email
    ) {
        boolean exists = userService.findByEmail(email).isPresent();
        return ResponseEntity.ok(
                new ApiResponse<>(true, "OK", exists, 200)
        );
    }

    @PutMapping("/user/{userId}/toggle-status")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserStatus(@PathVariable Long userId) {
        ApiResponse<UserResponse> response = adminService.toggleUserStatus(userId);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.NOT_FOUND)
                .body(response);
    }
}
