package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.Role.RoleCreateDto;
import com.example.project_backend04.dto.request.Role.RoleUpdateDto;
import com.example.project_backend04.dto.request.User.CreateUserRequest;
import com.example.project_backend04.dto.request.User.UpdateUserRequest;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.dto.response.User.UserResponse;
import com.example.project_backend04.entity.Role;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.mapper.UserMapper;
import com.example.project_backend04.repository.RoleRepository;
import com.example.project_backend04.repository.UserRepository;
import com.example.project_backend04.service.IService.IAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminService implements IAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final EmailService emailService;


    //    RoleManager
@Override
public ApiResponse<Role> createRole(RoleCreateDto request) {
    if (roleRepository.findRoleByName(request.getName()).isPresent()) {
        return new ApiResponse<>(false, "Role đã tồn tại", null, 400);
    }

    Role role = new Role();
    role.setName(request.getName());
    role.setDescription(request.getDescription());
    roleRepository.save(role);

    return new ApiResponse<>(true, "Tạo role thành công", role, 200);
}


    @Override
        public ApiResponse<List<Role>> getAllRoles() {
            List<Role> roles = roleRepository.findAll();
            if (roles.isEmpty()) {
                return new ApiResponse<>(false, "Không có role nào trong hệ thống", null, 404);
            }

            return new ApiResponse<>(true, "Lấy danh sách role thành công", roles, 200);
        }

        @Override
        public ApiResponse<Role> updateRole(RoleUpdateDto dto) {
            Optional<Role> existingRoleOpt = roleRepository.findById(dto.getId());
            if (existingRoleOpt.isEmpty()) {
                return new ApiResponse<>(false, "Role không tồn tại", null, 404);
            }
            Role existingRole = existingRoleOpt.get();
            Optional<Role> duplicateRole = roleRepository.findRoleByName(dto.getName());
            if (duplicateRole.isPresent() && !duplicateRole.get().getId().equals(dto.getId())) {
                return new ApiResponse<>(false, "Tên role này đã tồn tại", null, 400);
            }
            existingRole.setName(dto.getName());
            existingRole.setDescription(dto.getDescription());
            roleRepository.save(existingRole);
            return new ApiResponse<>(true, "Cập nhật role thành công", existingRole, 200);
        }

        @Override
        public ApiResponse<Void> deleteRole(Long id) {
            Optional<Role> roleOpt = roleRepository.findById(id);

            if (roleOpt.isEmpty()) {
                return new ApiResponse<>(false, "Role không tồn tại", null, 404);
            }

            Role role = roleOpt.get();
            if ("ADMIN".equals(role.getName())) {
                return new ApiResponse<>(false, "Không thể xóa role ADMIN", null, 403);
            }

            roleRepository.delete(role);

            return new ApiResponse<>(true, "Xóa role thành công", null, 200);
        }



    //      UserManager
    @Transactional
    public ApiResponse<UserResponse> createUser(CreateUserRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            return new ApiResponse<>(false, "Email đã tồn tại", null, 400);
        }

        // Tạo mật khẩu ngẫu nhiên
        String randomPassword = generateRandomPassword();

        // Dùng mapper thay vì tạo thủ công
        User user = userMapper.toEntity(req);
        user.setPassword(passwordEncoder.encode(randomPassword));

        Role role = req.getRoleId() != null
                ? roleRepository.findById(req.getRoleId()).orElse(null)
                : roleRepository.findRoleByName("USER")
                .orElseGet(() -> roleRepository.save(new Role(null, "USER", null)));
        user.setRole(role);

        User saved = userRepository.save(user);

        // Gửi email với mật khẩu
        try {
            emailService.sendPasswordEmail(req.getEmail(), req.getFullName(), randomPassword);
        } catch (Exception e) {
            // Log error nhưng vẫn trả về success vì user đã được tạo
            System.err.println("Failed to send email: " + e.getMessage());
        }

        return new ApiResponse<>(true, "Tạo user thành công. Mật khẩu đã được gửi qua email.", userMapper.toResponse(saved), 201);
    }

    private String generateRandomPassword() {
        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%^&*";
        String allChars = upperCase + lowerCase + digits + special;

        StringBuilder password = new StringBuilder();
        java.util.Random random = new java.util.Random();

        // Đảm bảo có ít nhất 1 ký tự mỗi loại
        password.append(upperCase.charAt(random.nextInt(upperCase.length())));
        password.append(lowerCase.charAt(random.nextInt(lowerCase.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(special.charAt(random.nextInt(special.length())));

        // Thêm 8 ký tự ngẫu nhiên nữa (tổng 12 ký tự)
        for (int i = 0; i < 8; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Shuffle để không có pattern cố định
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }

        return new String(passwordArray);
    }

    @Transactional
    public ApiResponse<UserResponse> updateUser(Long id, UpdateUserRequest req) {
        Optional<User> existingOpt = userRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return new ApiResponse<>(false, "User không tồn tại", null, 404);
        }

        User existing = existingOpt.get();
        userMapper.updateEntityFromRequest(req, existing);

        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(req.getPassword()));
        }

        if (req.getRoleId() != null) {
            roleRepository.findById(req.getRoleId()).ifPresent(existing::setRole);
        }

        User saved = userRepository.save(existing);
        return new ApiResponse<>(true, "Cập nhật user thành công", userMapper.toResponse(saved), 200);
    }

    @Transactional
    public ApiResponse<Void> deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            return new ApiResponse<>(false, "User không tồn tại", null, 404);
        }
        userRepository.deleteById(id);
        return new ApiResponse<>(true, "Xóa user thành công", null, 200);
    }

    @Override
    public ApiResponse<List<UserResponse>> getAllUsers() {
        List<User> users = userRepository.findAll();

        List<UserResponse> userResponses = users.stream()
                .map(userMapper::toResponse)
                .toList();

        return new ApiResponse<>(true, "Danh sách user", userResponses, 200);
    }

}
