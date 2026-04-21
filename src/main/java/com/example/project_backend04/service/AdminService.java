package com.example.project_backend04.service;

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
import com.example.project_backend04.entity.User;
import com.example.project_backend04.entity.Membership;
import com.example.project_backend04.entity.ServiceRegistration;
import com.example.project_backend04.entity.TrainerSpecialty;
import com.example.project_backend04.event.EntityChangedEvent;
import com.example.project_backend04.mapper.UserMapper;
import com.example.project_backend04.repository.RoleRepository;
import com.example.project_backend04.repository.UserRepository;
import com.example.project_backend04.repository.MembershipRepository;
import com.example.project_backend04.repository.ServiceRegistrationRepository;
import com.example.project_backend04.repository.TrainerSpecialtyRepository;
import com.example.project_backend04.service.IService.IAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService implements IAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MembershipRepository membershipRepository;
    private final ServiceRegistrationRepository serviceRegistrationRepository;
    private final TrainerSpecialtyRepository trainerSpecialtyRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher;


    //    RoleManager
    @Override
    public ApiResponse<Role> createRole(RoleCreateDto request) {
        if (roleRepository.findRoleByName(request.getName()).isPresent()) {
            return new ApiResponse<>(false, "Role already exists", null, 400);
        }

        Role role = new Role();
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        roleRepository.save(role);

        return new ApiResponse<>(true, "Role created successfully", role, 200);
    }

    @Override
    public ApiResponse<List<Role>> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        if (roles.isEmpty()) {
            return new ApiResponse<>(false, "No roles found in the system", null, 404);
        }

        return new ApiResponse<>(true, "Retrieved role list successfully", roles, 200);
    }

    @Override
    public ApiResponse<Page<Role>> getAllRoles(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Role> rolePage = roleRepository.findAll(pageable);
        return new ApiResponse<>(true, "Retrieved role list successfully", rolePage, 200);
    }

    @Override
    public ApiResponse<Role> updateRole(RoleUpdateDto dto) {
        Optional<Role> existingRoleOpt = roleRepository.findById(dto.getId());
        if (existingRoleOpt.isEmpty()) {
            return new ApiResponse<>(false, "Role not found", null, 404);
        }

        Role existingRole = existingRoleOpt.get();

        Optional<Role> duplicateRole = roleRepository.findRoleByName(dto.getName());
        if (duplicateRole.isPresent() && !duplicateRole.get().getId().equals(dto.getId())) {
            return new ApiResponse<>(false, "Role name already exists", null, 400);
        }

        existingRole.setName(dto.getName());
        existingRole.setDescription(dto.getDescription());
        roleRepository.save(existingRole);

        return new ApiResponse<>(true, "Role updated successfully", existingRole, 200);
    }

    @Override
    public ApiResponse<Void> deleteRole(Long id) {
        Optional<Role> roleOpt = roleRepository.findById(id);

        if (roleOpt.isEmpty()) {
            return new ApiResponse<>(false, "Role not found", null, 404);
        }

        Role role = roleOpt.get();
        if ("ADMIN".equals(role.getName())) {
            return new ApiResponse<>(false, "Cannot delete ADMIN role", null, 403);
        }

        roleRepository.delete(role);

        return new ApiResponse<>(true, "Role deleted successfully", null, 200);
    }



    //      UserManager
    @Transactional
    public ApiResponse<UserResponse> createUser(CreateUserRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            return new ApiResponse<>(false, "Email already exists", null, 400);
        }
        
        if (req.getDateOfBirth() != null && !req.getDateOfBirth().trim().isEmpty()) {
            try {
                LocalDate birthDate = LocalDate.parse(req.getDateOfBirth());
                LocalDate today = LocalDate.now();
                int age = Period.between(birthDate, today).getYears();
                
                if (age < 16) {
                    return new ApiResponse<>(false, "User must be at least 16 years old to register", null, 400);
                }
            } catch (Exception e) {
                return new ApiResponse<>(false, "Invalid date of birth format", null, 400);
            }
        }

        String randomPassword = generateRandomPassword();
        User user = userMapper.toEntity(req);
        user.setPassword(passwordEncoder.encode(randomPassword));

        Role role = req.getRoleId() != null
                ? roleRepository.findById(req.getRoleId()).orElse(null)
                : roleRepository.findRoleByName("USER")
                .orElseGet(() -> roleRepository.save(new Role(null, "USER", null)));
        user.setRole(role);
        User saved = userRepository.save(user);
        
        try {
            emailService.sendPasswordEmail(req.getEmail(), req.getFullName(), randomPassword);
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }

        eventPublisher.publishEvent(
            new EntityChangedEvent(this, "USER", "CREATED", userMapper.toResponse(saved), saved.getId())
        );

        UserResponse response = userMapper.toResponse(saved);
        return new ApiResponse<>(true, "User created successfully. Password sent via email.", response, 201);
    }

    private String generateRandomPassword() {
        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%^&*";
        String allChars = upperCase + lowerCase + digits + special;

        StringBuilder password = new StringBuilder();
        java.util.Random random = new java.util.Random();

        password.append(upperCase.charAt(random.nextInt(upperCase.length())));
        password.append(lowerCase.charAt(random.nextInt(lowerCase.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(special.charAt(random.nextInt(special.length())));

        for (int i = 0; i < 8; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

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
            return new ApiResponse<>(false, "User not found", null, 404);
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
        
        eventPublisher.publishEvent(
            new EntityChangedEvent(this, "USER", "UPDATED", userMapper.toResponse(saved), saved.getId())
        );
        
        return new ApiResponse<>(true, "User updated successfully", userMapper.toResponse(saved), 200);
    }

//    @Transactional
//    public ApiResponse<Void> deleteUser(Long id) {
//        if (!userRepository.existsById(id)) {
//            return new ApiResponse<>(false, "User không tồn tại", null, 404);
//        }
//        eventPublisher.publishEvent(
//            new EntityChangedEvent(this, "USER", "DELETED", null, id)
//        );
//
//        userRepository.deleteById(id);
//        return new ApiResponse<>(true, "Xóa user thành công", null, 200);
//    }

    @Transactional
    public ApiResponse<Void> deleteUser(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return new ApiResponse<>(false, "User not found", null, 404);
        }
        
        user.setIsActive(false);
        userRepository.save(user);
        
        eventPublisher.publishEvent(
            new EntityChangedEvent(this, "USER", "DELETED", null, id)
        );

        return new ApiResponse<>(true, "User disabled successfully", null, 200);
    }

    @Override
    public ApiResponse<Page<UserResponse>> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createDate").descending());

        Page<User> userPage = userRepository.findByRoleNameIn(
            List.of("USER", "STAFF"), 
            pageable
        );

        Page<UserResponse> responsePage = userPage.map(userMapper::toResponse);

        return new ApiResponse<>(true, "User list retrieved successfully", responsePage, 200);
    }

    @Override
    public ApiResponse<Page<UserResponse>> getUsersByRole(String roleName, int page, int size) {
        if (!List.of("USER", "STAFF").contains(roleName)) {
            return new ApiResponse<>(false, "Invalid role name", null, 400);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createDate").descending());
        Page<User> userPage = userRepository.findByRoleName(roleName, pageable);
        Page<UserResponse> responsePage = userPage.map(userMapper::toResponse);

        return new ApiResponse<>(true, "Users by role retrieved successfully", responsePage, 200);
    }

    @Override
    public ApiResponse<Map<String, Long>> getUserCounts() {
        long userCount = userRepository.countByRoleName("USER");
        long staffCount = userRepository.countByRoleName("STAFF");
        long totalCount = userCount + staffCount;

        Map<String, Long> counts = Map.of(
            "USER", userCount,
            "STAFF", staffCount,
            "TOTAL", totalCount
        );

        return new ApiResponse<>(true, "User counts retrieved successfully", counts, 200);
    }

    @Override
    public ApiResponse<Page<UserResponse>> searchUsers(String searchTerm, String role, int page, int size) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return new ApiResponse<>(false, "Search term cannot be empty", null, 400);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createDate").descending());
        Page<User> userPage;

        if ("ALL".equals(role)) {
            userPage = userRepository.searchByEmailOrPhoneInRoles(
                List.of("USER", "STAFF"), 
                searchTerm.trim(), 
                pageable
            );
        } else if (List.of("USER", "STAFF").contains(role)) {
            userPage = userRepository.searchByEmailOrPhoneInRole(
                role, 
                searchTerm.trim(), 
                pageable
            );
        } else {
            return new ApiResponse<>(false, "Invalid role name", null, 400);
        }

        Page<UserResponse> responsePage = userPage.map(userMapper::toResponse);
        return new ApiResponse<>(true, "Search results retrieved successfully", responsePage, 200);
    }

    // User detail methods
    @Override
    public ApiResponse<UserDetailResponse> getUserDetail(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return new ApiResponse<>(false, "User not found", null, 404);
        }

        User user = userOpt.get();
        UserDetailResponse response = UserDetailResponse.builder()
                .id(user.getId())
                .username(user.getEmail())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .dateOfBirth(user.getDateOfBirth())
                .avatar(user.getAvatar())
                .bio(user.getBio())
                .coverPhoto(user.getCoverPhoto())
                .createDate(user.getCreateDate())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .totalExperienceYears(user.getTotalExperienceYears())
                .education(user.getEducation())
                .emergencyContact(user.getEmergencyContact())
                .emergencyPhone(user.getEmergencyPhone())
                .build();

        return new ApiResponse<>(true, "User details retrieved successfully", response, 200);
    }

    @Override
    public ApiResponse<List<UserMembershipResponse>> getUserMemberships(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return new ApiResponse<>(false, "User not found", null, 404);
        }

        User user = userOpt.get();
        List<Membership> memberships = membershipRepository.findByUserOrderByCreateDateDesc(user);

        List<UserMembershipResponse> responses = memberships.stream()
                .map(membership -> UserMembershipResponse.builder()
                        .id(membership.getId())
                        .membershipPackage(UserMembershipResponse.MembershipPackageInfo.builder()
                                .id(membership.getMembershipPackage().getId())
                                .name(membership.getMembershipPackage().getName())
                                .description(membership.getMembershipPackage().getDescription())
                                .duration(membership.getMembershipPackage().getDuration())
                                .price(membership.getMembershipPackage().getPrice())
                                .build())
                        .startDate(membership.getStartDate())
                        .endDate(membership.getEndDate())
                        .isActive(membership.isActive())
                        .paidAmount(membership.getPaidAmount())
                        .status(membership.getStatus().name())
                        .paymentMethod(membership.getPaymentMethod().name())
                        .registrationDate(membership.getCreateDate().toString())
                        .build())
                .collect(Collectors.toList());

        return new ApiResponse<>(true, "Memberships retrieved successfully", responses, 200);
    }

    @Override
    public ApiResponse<List<UserServiceRegistrationResponse>> getUserServiceRegistrations(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return new ApiResponse<>(false, "User not found", null, 404);
        }

        User user = userOpt.get();
        List<ServiceRegistration> registrations = serviceRegistrationRepository.findByUserWithGymServiceOrderByRegistrationDateDesc(user);

        List<UserServiceRegistrationResponse> responses = registrations.stream()
                .map(registration -> {
                    UserServiceRegistrationResponse.ServiceInfo serviceInfo = UserServiceRegistrationResponse.ServiceInfo.builder()
                            .id(registration.getGymService().getId())
                            .name(registration.getGymService().getName())
                            .description(registration.getGymService().getDescription())
                            .price(registration.getGymService().getPrice())
                            .category(UserServiceRegistrationResponse.CategoryInfo.builder()
                                    .id(registration.getGymService().getCategory().getId())
                                    .name(registration.getGymService().getCategory().getName())
                                    .displayName(registration.getGymService().getCategory().getDisplayName())
                                    .build())
                            .build();

                    UserServiceRegistrationResponse response = UserServiceRegistrationResponse.builder()
                            .id(registration.getId())
                            .service(serviceInfo)
                            .registrationDate(registration.getRegistrationDate())
                            .expirationDate(registration.getExpirationDate())
                            .status(registration.getActualStatus().name())
                            .notes(registration.getNotes())
                            .trainer(registration.getTrainer() != null ? 
                                UserServiceRegistrationResponse.TrainerInfo.builder()
                                    .id(registration.getTrainer().getId())
                                    .fullName(registration.getTrainer().getFullName())
                                    .avatar(registration.getTrainer().getAvatar())
                                    .build() : null)
                            .build();
                    
                    return response;
                })
                .collect(Collectors.toList());

        return new ApiResponse<>(true, "Service registrations retrieved successfully", responses, 200);
    }

    @Override
    public ApiResponse<List<TrainerSpecialtyResponse>> getTrainerSpecialties(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return new ApiResponse<>(false, "User not found", null, 404);
        }

        User user = userOpt.get();
        
        if (!user.isTrainer()) {
            return new ApiResponse<>(false, "User is not a trainer", null, 400);
        }

        List<TrainerSpecialty> specialties = trainerSpecialtyRepository.findByUserAndIsActiveTrueOrderBySpecialtyAsc(user);

        List<TrainerSpecialtyResponse> responses = specialties.stream()
                .map(specialty -> TrainerSpecialtyResponse.builder()
                        .id(specialty.getId())
                        .specialty(TrainerSpecialtyResponse.SpecialtyInfo.builder()
                                .id(specialty.getSpecialty().getId())
                                .name(specialty.getSpecialty().getName())
                                .displayName(specialty.getSpecialty().getDisplayName())
                                .build())
                        .description(specialty.getDescription())
                        .experienceYears(specialty.getExperienceYears())
                        .certifications(specialty.getCertifications())
                        .level(specialty.getLevel())
                        .isActive(specialty.getIsActive())
                        .build())
                .collect(Collectors.toList());

        return new ApiResponse<>(true, "Trainer specialties retrieved successfully", responses, 200);
    }

    @Override
    @Transactional
    public ApiResponse<UserResponse> toggleUserStatus(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return new ApiResponse<>(false, "User not found", null, 404);
        }
        User user = userOpt.get();
        user.setIsActive(!user.getIsActive());
        User savedUser = userRepository.save(user);
        String action = savedUser.getIsActive() ? "ACTIVATED" : "DEACTIVATED";
        eventPublisher.publishEvent(
            new EntityChangedEvent(this, "USER", action, userMapper.toResponse(savedUser), savedUser.getId())
        );
        
        String message = savedUser.getIsActive() ? "User activated successfully" : "User deactivated successfully";
        return new ApiResponse<>(true, message, userMapper.toResponse(savedUser), 200);
    }

}
