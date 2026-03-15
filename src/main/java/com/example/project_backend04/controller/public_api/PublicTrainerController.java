package com.example.project_backend04.controller.public_api;

import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.dto.response.Trainer.TrainerResponse;
import com.example.project_backend04.service.IService.ITrainerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/trainers")
@RequiredArgsConstructor
public class PublicTrainerController {

    private final ITrainerService trainerService;

    /**
     * Lấy thông tin trainer theo ID (public)
     */
    @GetMapping("/{trainerId}")
    public ResponseEntity<ApiResponse<TrainerResponse>> getTrainerById(@PathVariable Long trainerId) {
        ApiResponse<TrainerResponse> response = trainerService.getTrainerById(trainerId);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.NOT_FOUND)
                .body(response);
    }

    /**
     * Lấy danh sách tất cả trainers với phân trang (public)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<TrainerResponse>>> getAllTrainers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        ApiResponse<Page<TrainerResponse>> response = trainerService.getAllTrainers(page, size);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Lấy trainers theo specialty (public)
     */
    @GetMapping("/specialty/{specialty}")
    public ResponseEntity<ApiResponse<List<TrainerResponse>>> getTrainersBySpecialty(
            @PathVariable String specialty) {
        
        ApiResponse<List<TrainerResponse>> response = trainerService.getTrainersBySpecialty(specialty);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Lấy danh sách specialties có sẵn
     */
    @GetMapping("/specialties")
    public ResponseEntity<ApiResponse<List<String>>> getAvailableSpecialties() {
        try {
            List<String> specialties = List.of(
                "PERSONAL_TRAINER",
                "BOXING", 
                "YOGA",
                "CARDIO",
                "GYM",
                "OTHER"
            );
            
            ApiResponse<List<String>> response = new ApiResponse<>(
                true, 
                "Lấy danh sách specialties thành công", 
                specialties, 
                200
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ApiResponse<List<String>> response = new ApiResponse<>(
                false, 
                "Lỗi khi lấy specialties: " + e.getMessage(), 
                null, 
                500
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}