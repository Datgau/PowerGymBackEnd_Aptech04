package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.Trainer.CreateTrainerRequest;
import com.example.project_backend04.dto.request.Trainer.UploadTrainerDocumentRequest;
import com.example.project_backend04.dto.response.ServiceCategory.ServiceCategoryResponse;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.dto.response.Trainer.TrainerDocumentResponse;
import com.example.project_backend04.dto.response.Trainer.TrainerResponse;
import com.example.project_backend04.dto.response.Trainer.TrainerSpecialtyResponse;
import com.example.project_backend04.entity.*;
import com.example.project_backend04.entity.ServiceCategory;
import com.example.project_backend04.repository.*;
import com.example.project_backend04.service.IService.ICloudinaryService;
import com.example.project_backend04.service.IService.ITrainerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TrainerService implements ITrainerService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TrainerSpecialtyRepository trainerSpecialtyRepository;
    private final TrainerDocumentRepository trainerDocumentRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final ICloudinaryService cloudinaryService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public ApiResponse<TrainerResponse> createTrainer(CreateTrainerRequest request) {
        try {
            // Validate email uniqueness
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                return new ApiResponse<>(false, "Email đã tồn tại trong hệ thống", null, 400);
            }
            
            // Validate trainer role exists
            Role trainerRole = roleRepository.findRoleByName("TRAINER")
                    .orElseThrow(() -> new RuntimeException("Role TRAINER không tồn tại"));
            
            // Validate all specialties exist
            for (CreateTrainerRequest.TrainerSpecialtyRequest spec : request.getSpecialties()) {
                if (!serviceCategoryRepository.existsById(spec.getSpecialtyId())) {
                    return new ApiResponse<>(false, "Specialty không tồn tại với ID: " + spec.getSpecialtyId(), null, 400);
                }
            }
            
            // Calculate total experience years from specialties (SUM of all years)
            Integer calculatedTotalExperience = request.getSpecialties().stream()
                    .map(CreateTrainerRequest.TrainerSpecialtyRequest::getExperienceYears)
                    .filter(years -> years != null && years > 0)
                    .reduce(0, Integer::sum);
            
            String randomPassword = generateRandomPassword();
            
            // Create trainer user
            User trainer = new User();
            trainer.setEmail(request.getEmail());
            trainer.setFullName(request.getFullName());
            trainer.setPhoneNumber(request.getPhoneNumber());
            trainer.setBio(request.getBio());
            trainer.setTotalExperienceYears(calculatedTotalExperience); // Use calculated value
            trainer.setEducation(request.getEducation());
            trainer.setEmergencyContact(request.getEmergencyContact());
            trainer.setEmergencyPhone(request.getEmergencyPhone());
            trainer.setPassword(passwordEncoder.encode(randomPassword));
            trainer.setRole(trainerRole);
            trainer.setIsActive(true);
            
            User savedTrainer = userRepository.save(trainer);
            
            // Create trainer specialties
            List<TrainerSpecialty> specialties = request.getSpecialties().stream()
                    .map(spec -> {
                        // Tìm ServiceCategory theo specialtyId
                        ServiceCategory serviceCategory = serviceCategoryRepository.findById(spec.getSpecialtyId())
                                .orElseThrow(() -> new RuntimeException("Specialty không tồn tại với ID: " + spec.getSpecialtyId()));
                        
                        TrainerSpecialty specialty = new TrainerSpecialty();
                        specialty.setUser(savedTrainer);
                        specialty.setSpecialty(serviceCategory); // Set ServiceCategory
                        specialty.setDescription(spec.getDescription());
                        specialty.setExperienceYears(spec.getExperienceYears());
                        specialty.setCertifications(spec.getCertifications());
                        specialty.setLevel(spec.getLevel());
                        specialty.setIsActive(true);
                        return specialty;
                    })
                    .collect(Collectors.toList());

            trainerSpecialtyRepository.saveAll(specialties);
            
            // Send password email (non-blocking)
            try {
                emailService.sendPasswordEmail(request.getEmail(), request.getFullName(), randomPassword);
            } catch (Exception e) {
                System.err.println("Failed to send email: " + e.getMessage());
            }
            
            TrainerResponse response = mapToTrainerResponse(savedTrainer);
            return new ApiResponse<>(true, "Tạo trainer thành công. Mật khẩu đã được gửi qua email.", response, 201);

        } catch (Exception e) {
            System.err.println("Error creating trainer: " + e.getMessage());
            e.printStackTrace();
            return new ApiResponse<>(false, "Lỗi khi tạo trainer: " + e.getMessage(), null, 500);
        }
    }

    @Override
    public ApiResponse<String> uploadTrainerAvatar(Long trainerId, MultipartFile file) {
        try {
            User trainer = findTrainerById(trainerId);
            if (trainer == null) {
                return new ApiResponse<>(false, "Không tìm thấy trainer", null, 404);
            }

            String avatarUrl = cloudinaryService.uploadSingleFile(file, "trainer-avatars");
            trainer.setAvatar(avatarUrl);
            userRepository.save(trainer);

            return new ApiResponse<>(true, "Upload avatar thành công", avatarUrl, 200);
        } catch (IOException e) {
            return new ApiResponse<>(false, "Lỗi upload avatar: " + e.getMessage(), null, 500);
        }
    }

    @Override
    public ApiResponse<String> uploadTrainerCoverPhoto(Long trainerId, MultipartFile file) {
        try {
            User trainer = findTrainerById(trainerId);
            if (trainer == null) {
                return new ApiResponse<>(false, "Không tìm thấy trainer", null, 404);
            }

            String coverUrl = cloudinaryService.uploadSingleFile(file, "trainer-covers");
            trainer.setCoverPhoto(coverUrl);
            userRepository.save(trainer);

            return new ApiResponse<>(true, "Upload cover photo thành công", coverUrl, 200);
        } catch (IOException e) {
            return new ApiResponse<>(false, "Lỗi upload cover photo: " + e.getMessage(), null, 500);
        }
    }
    @Override
    public ApiResponse<TrainerDocumentResponse> uploadTrainerDocument(
            Long trainerId, MultipartFile file, UploadTrainerDocumentRequest request) {
        try {
            User trainer = findTrainerById(trainerId);
            if (trainer == null) {
                return new ApiResponse<>(false, "Không tìm thấy trainer", null, 404);
            }

            // Upload file lên Cloudinary
            String fileUrl = cloudinaryService.uploadSingleFile(file, "trainer-documents");

            // Tạo TrainerDocument
            TrainerDocument document = new TrainerDocument();
            document.setUser(trainer);
            document.setDocumentType(request.getDocumentType());
            document.setFileName(file.getOriginalFilename());
            document.setFileUrl(fileUrl);
            document.setDescription(request.getDescription());
            document.setExpiryDate(request.getExpiryDate());
            document.setIsVerified(false);
            document.setIsActive(true);

            TrainerDocument savedDocument = trainerDocumentRepository.save(document);

            // Map to response
            TrainerDocumentResponse response = mapToDocumentResponse(savedDocument);
            return new ApiResponse<>(true, "Upload document thành công", response, 201);

        } catch (IOException e) {
            return new ApiResponse<>(false, "Lỗi upload document: " + e.getMessage(), null, 500);
        }
    }

    @Override
    public ApiResponse<TrainerResponse> getTrainerById(Long trainerId) {
        User trainer = findTrainerById(trainerId);
        if (trainer == null) {
            return new ApiResponse<>(false, "Không tìm thấy trainer", null, 404);
        }

        TrainerResponse response = mapToTrainerResponse(trainer);
        return new ApiResponse<>(true, "Lấy thông tin trainer thành công", response, 200);
    }

    @Override
    public ApiResponse<Page<TrainerResponse>> getAllTrainers(int page, int size) {
        try {
            Role trainerRole = roleRepository.findRoleByName("TRAINER")
                    .orElseThrow(() -> new RuntimeException("Role TRAINER không tồn tại"));

            Pageable pageable = PageRequest.of(page, size, Sort.by("createDate").descending());
            // Lấy tất cả trainers bao gồm cả inactive
            Page<User> trainers = userRepository.findByRole(trainerRole, pageable);

            Page<TrainerResponse> response = trainers.map(this::mapToTrainerResponse);
            return new ApiResponse<>(true, "Lấy danh sách trainers thành công", response, 200);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi lấy danh sách trainers: " + e.getMessage(), null, 500);
        }
    }

    @Override
    public ApiResponse<Page<TrainerResponse>> searchTrainers(String searchTerm, int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createDate").descending());
            
            // Search by email or phone number using existing repository method
            Page<User> trainers = userRepository.searchByEmailOrPhoneInRole("TRAINER", searchTerm, pageable);

            Page<TrainerResponse> response = trainers.map(this::mapToTrainerResponse);
            return new ApiResponse<>(true, "Tìm kiếm trainers thành công", response, 200);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi tìm kiếm trainers: " + e.getMessage(), null, 500);
        }
    }

    @Override
    public List<TrainerResponse> getAllActiveTrainers() {
        try {
            Role trainerRole = roleRepository.findRoleByName("TRAINER")
                    .orElseThrow(() -> new RuntimeException("Role TRAINER không tồn tại"));

            List<User> trainers = userRepository.findByRoleAndIsActiveTrueOrderByCreateDateDesc(trainerRole);
            
            return trainers.stream()
                    .map(this::mapToTrainerResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy danh sách trainers: " + e.getMessage());
        }
    }

    @Override
    public ApiResponse<List<TrainerResponse>> getTrainersBySpecialty(String specialty) {
        try {

            ServiceCategory serviceCategory = serviceCategoryRepository
                    .findByNameIgnoreCase(specialty)
                    .orElseThrow(() -> new IllegalArgumentException("Specialty không tồn tại"));

            List<User> trainers = trainerSpecialtyRepository.findTrainersByCategory(serviceCategory);

            List<TrainerResponse> response = trainers.stream()
                    .map(this::mapToTrainerResponse)
                    .collect(Collectors.toList());

            return new ApiResponse<>(true, "Lấy trainers theo specialty thành công", response, 200);

        } catch (IllegalArgumentException e) {
            return new ApiResponse<>(false, e.getMessage(), null, 400);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi lấy trainers: " + e.getMessage(), null, 500);
        }
    }

    @Override
    public ApiResponse<String> verifyTrainerDocument(Long documentId, Boolean isVerified) {
        try {
            TrainerDocument document = trainerDocumentRepository.findById(documentId)
                    .orElse(null);

            if (document == null) {
                return new ApiResponse<>(false, "Không tìm thấy document", null, 404);
            }

            document.setIsVerified(isVerified);
            trainerDocumentRepository.save(document);

            String message = isVerified ? "Xác minh document thành công" : "Hủy xác minh document thành công";
            return new ApiResponse<>(true, message, null, 200);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi verify document: " + e.getMessage(), null, 500);
        }
    }

    @Override
    public ApiResponse<String> deleteTrainerDocument(Long trainerId, Long documentId) {
        try {
            User trainer = findTrainerById(trainerId);
            if (trainer == null) {
                return new ApiResponse<>(false, "Không tìm thấy trainer", null, 404);
            }

            TrainerDocument document = trainerDocumentRepository
                    .findByIdAndUserAndIsActiveTrue(documentId, trainer)
                    .orElse(null);

            if (document == null) {
                return new ApiResponse<>(false, "Không tìm thấy document", null, 404);
            }

            // Soft delete
            document.setIsActive(false);
            trainerDocumentRepository.save(document);

            return new ApiResponse<>(true, "Xóa document thành công", null, 200);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi xóa document: " + e.getMessage(), null, 500);
        }
    }

    @Override
    public ApiResponse<TrainerResponse> updateTrainer(Long trainerId, CreateTrainerRequest request) {
        try {
            User trainer = findTrainerById(trainerId);
            if (trainer == null) {
                return new ApiResponse<>(false, "Không tìm thấy trainer", null, 404);
            }

            // Validate all specialties exist before processing
            for (CreateTrainerRequest.TrainerSpecialtyRequest spec : request.getSpecialties()) {
                if (!serviceCategoryRepository.existsById(spec.getSpecialtyId())) {
                    return new ApiResponse<>(false, "Specialty không tồn tại với ID: " + spec.getSpecialtyId(), null, 400);
                }
            }

            // Calculate total experience years from specialties (SUM of all years)
            Integer calculatedTotalExperience = request.getSpecialties().stream()
                    .map(CreateTrainerRequest.TrainerSpecialtyRequest::getExperienceYears)
                    .filter(years -> years != null && years > 0)
                    .reduce(0, Integer::sum);

            // Cập nhật thông tin cơ bản
            trainer.setFullName(request.getFullName());
            trainer.setPhoneNumber(request.getPhoneNumber());
            trainer.setBio(request.getBio());
            trainer.setTotalExperienceYears(calculatedTotalExperience); // Use calculated value
            trainer.setEducation(request.getEducation());
            trainer.setEmergencyContact(request.getEmergencyContact());
            trainer.setEmergencyPhone(request.getEmergencyPhone());

            User updatedTrainer = userRepository.save(trainer);

            // Cập nhật specialties (xóa cũ, thêm mới)
            List<TrainerSpecialty> oldSpecialties = trainerSpecialtyRepository
                    .findByUserAndIsActiveTrueOrderBySpecialtyAsc(trainer);

            // Hard delete old specialties to avoid unique constraint violation
            if (!oldSpecialties.isEmpty()) {
                trainerSpecialtyRepository.deleteAll(oldSpecialties);
                trainerSpecialtyRepository.flush(); // Force delete before insert
            }

            // Add new specialties
            List<TrainerSpecialty> newSpecialties = request.getSpecialties().stream()
                    .map(spec -> {
                        // Tìm ServiceCategory theo specialtyId (đã validate ở trên nên không cần orElseThrow)
                        ServiceCategory serviceCategory = serviceCategoryRepository.findById(spec.getSpecialtyId()).get();
                        
                        TrainerSpecialty specialty = new TrainerSpecialty();
                        specialty.setUser(updatedTrainer);
                        specialty.setSpecialty(serviceCategory); // Set ServiceCategory
                        specialty.setDescription(spec.getDescription());
                        specialty.setExperienceYears(spec.getExperienceYears());
                        specialty.setCertifications(spec.getCertifications());
                        specialty.setLevel(spec.getLevel());
                        specialty.setIsActive(true);
                        return specialty;
                    })
                    .collect(Collectors.toList());

            trainerSpecialtyRepository.saveAll(newSpecialties);
            trainerSpecialtyRepository.flush(); // Ensure specialties are persisted

            // Refresh trainer to get updated data with new specialties
            User refreshedTrainer = userRepository.findById(trainerId).orElse(updatedTrainer);
            
            TrainerResponse response = mapToTrainerResponse(refreshedTrainer);
            return new ApiResponse<>(true, "Cập nhật trainer thành công", response, 200);

        } catch (Exception e) {
            System.err.println("Error updating trainer: " + e.getMessage());
            e.printStackTrace();
            return new ApiResponse<>(false, "Lỗi khi cập nhật trainer: " + e.getMessage(), null, 500);
        }
    }

    @Override
    public ApiResponse<String> deactivateTrainer(Long trainerId) {
        try {
            User trainer = findTrainerById(trainerId);
            if (trainer == null) {
                return new ApiResponse<>(false, "Không tìm thấy trainer", null, 404);
            }

            trainer.setIsActive(false);
            userRepository.save(trainer);

            return new ApiResponse<>(true, "Deactivate trainer thành công", null, 200);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi deactivate trainer: " + e.getMessage(), null, 500);
        }
    }

    // Helper methods
    private User findTrainerById(Long trainerId) {
        return userRepository.findById(trainerId)
                .filter(user -> user.getRole() != null && "TRAINER".equals(user.getRole().getName()))
                .orElse(null);
    }

    private TrainerResponse mapToTrainerResponse(User trainer) {
        TrainerResponse response = new TrainerResponse();
        response.setId(trainer.getId());
        response.setEmail(trainer.getEmail());
        response.setFullName(trainer.getFullName());
        response.setPhoneNumber(trainer.getPhoneNumber());
        response.setAvatar(trainer.getAvatar());
        response.setBio(trainer.getBio());
        response.setCoverPhoto(trainer.getCoverPhoto());
        response.setIsActive(trainer.getIsActive());
        response.setCreateDate(trainer.getCreateDate());

        // Map trainer-specific fields
        response.setTotalExperienceYears(trainer.getTotalExperienceYears());
        response.setEducation(trainer.getEducation());
        response.setEmergencyContact(trainer.getEmergencyContact());
        response.setEmergencyPhone(trainer.getEmergencyPhone());

        // Map specialties
        List<TrainerSpecialty> specialties = trainerSpecialtyRepository
                .findByUserAndIsActiveTrueOrderBySpecialtyAsc(trainer);

        List<TrainerSpecialtyResponse> specialtyResponses = specialties.stream()
                .map(this::mapToSpecialtyResponse)
                .collect(Collectors.toList());
        response.setSpecialties(specialtyResponses);

        // Map documents
        List<TrainerDocument> documents = trainerDocumentRepository
                .findByUserAndIsActiveTrueOrderByCreatedAtDesc(trainer);

        List<TrainerDocumentResponse> documentResponses = documents.stream()
                .map(this::mapToDocumentResponse)
                .collect(Collectors.toList());
        response.setDocuments(documentResponses);

        return response;
    }

    private TrainerSpecialtyResponse mapToSpecialtyResponse(TrainerSpecialty specialty) {
        TrainerSpecialtyResponse response = new TrainerSpecialtyResponse();
        response.setId(specialty.getId());

        // Map ServiceCategory entity to DTO to avoid lazy loading issues
        ServiceCategory category = specialty.getSpecialty();
        ServiceCategoryResponse categoryResponse = new ServiceCategoryResponse();
        categoryResponse.setId(category.getId());
        categoryResponse.setName(category.getName());
        categoryResponse.setDisplayName(category.getDisplayName());
        categoryResponse.setDescription(category.getDescription());
        categoryResponse.setIcon(category.getIcon());
        categoryResponse.setColor(category.getColor());
        categoryResponse.setIsActive(category.getIsActive());

        response.setSpecialty(categoryResponse); // Set the specialty field
        response.setDescription(specialty.getDescription());
        response.setExperienceYears(specialty.getExperienceYears());
        response.setCertifications(specialty.getCertifications());
        response.setLevel(specialty.getLevel());
        response.setIsActive(specialty.getIsActive());
        response.setCreatedAt(specialty.getCreatedAt());

        return response;
    }

    private TrainerDocumentResponse mapToDocumentResponse(TrainerDocument document) {
        TrainerDocumentResponse response = new TrainerDocumentResponse();
        response.setId(document.getId());
        response.setDocumentType(document.getDocumentType().name());
        response.setFileName(document.getFileName());
        response.setFileUrl(document.getFileUrl());
        response.setDescription(document.getDescription());
        response.setExpiryDate(document.getExpiryDate());
        response.setIsVerified(document.getIsVerified());
        response.setIsActive(document.getIsActive());
        response.setCreatedAt(document.getCreatedAt());
        return response;
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
}