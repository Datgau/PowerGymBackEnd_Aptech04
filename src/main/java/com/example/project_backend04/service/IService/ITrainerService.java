package com.example.project_backend04.service.IService;

import com.example.project_backend04.dto.request.Trainer.CreateTrainerRequest;
import com.example.project_backend04.dto.request.Trainer.UploadTrainerDocumentRequest;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.dto.response.Trainer.TrainerDocumentResponse;
import com.example.project_backend04.dto.response.Trainer.TrainerResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ITrainerService {

    // Tạo trainer mới với đầy đủ thông tin
    ApiResponse<TrainerResponse> createTrainer(CreateTrainerRequest request);

    // Upload avatar cho trainer
    ApiResponse<String> uploadTrainerAvatar(Long trainerId, MultipartFile file);

    // Upload cover photo cho trainer
    ApiResponse<String> uploadTrainerCoverPhoto(Long trainerId, MultipartFile file);

    // Upload documents cho trainer
    ApiResponse<TrainerDocumentResponse> uploadTrainerDocument(
            Long trainerId,
            MultipartFile file,
            UploadTrainerDocumentRequest request
    );

    // Lấy thông tin trainer
    ApiResponse<TrainerResponse> getTrainerById(Long trainerId);

    // Lấy danh sách trainers với phân trang
    ApiResponse<Page<TrainerResponse>> getAllTrainers(int page, int size);

    // Tìm kiếm trainers theo email hoặc phone number
    ApiResponse<Page<TrainerResponse>> searchTrainers(String searchTerm, int page, int size);

    // Lấy tất cả trainers đang hoạt động
    List<TrainerResponse> getAllActiveTrainers();

    // Lấy trainers theo specialty
    ApiResponse<List<TrainerResponse>> getTrainersBySpecialty(String specialty);

    // Verify document của trainer
    ApiResponse<String> verifyTrainerDocument(Long documentId, Boolean isVerified);

    // Xóa document của trainer
    ApiResponse<String> deleteTrainerDocument(Long trainerId, Long documentId);

    // Cập nhật thông tin trainer
    ApiResponse<TrainerResponse> updateTrainer(Long trainerId, CreateTrainerRequest request);

    // Deactivate trainer
    ApiResponse<String> deactivateTrainer(Long trainerId);
}