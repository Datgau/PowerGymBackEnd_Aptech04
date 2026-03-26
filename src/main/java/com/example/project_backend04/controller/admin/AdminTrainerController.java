package com.example.project_backend04.controller.admin;

import com.example.project_backend04.dto.request.Trainer.CreateTrainerRequest;
import com.example.project_backend04.dto.request.Trainer.UploadTrainerDocumentRequest;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.dto.response.Trainer.TrainerDocumentResponse;
import com.example.project_backend04.dto.response.Trainer.TrainerResponse;
import com.example.project_backend04.service.IService.ITrainerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/trainers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminTrainerController {

    private final ITrainerService trainerService;

    @PostMapping
    public ResponseEntity<ApiResponse<TrainerResponse>> createTrainer(
            @Valid @RequestBody CreateTrainerRequest request) {
        
        ApiResponse<TrainerResponse> response = trainerService.createTrainer(request);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST)
                .body(response);
    }


    @PostMapping(value = "/{trainerId}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadTrainerAvatar(
            @PathVariable Long trainerId,
            @RequestParam("file") MultipartFile file) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "File không được để trống", null, 400));
        }

        ApiResponse<String> response = trainerService.uploadTrainerAvatar(trainerId, file);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @PostMapping(value = "/{trainerId}/cover-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadTrainerCoverPhoto(
            @PathVariable Long trainerId,
            @RequestParam("file") MultipartFile file) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "File không được để trống", null, 400));
        }

        ApiResponse<String> response = trainerService.uploadTrainerCoverPhoto(trainerId, file);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @PostMapping(value = "/{trainerId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TrainerDocumentResponse>> uploadTrainerDocument(
            @PathVariable Long trainerId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "expiryDate", required = false) String expiryDate) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "File không được để trống", null, 400));
        }

        try {
            UploadTrainerDocumentRequest request = new UploadTrainerDocumentRequest();
            request.setDocumentType(com.example.project_backend04.enums.DocumentType.valueOf(documentType.toUpperCase()));
            request.setDescription(description);
            
            if (expiryDate != null && !expiryDate.isEmpty()) {
                request.setExpiryDate(java.time.LocalDateTime.parse(expiryDate));
            }

            ApiResponse<TrainerDocumentResponse> response =
                    trainerService.uploadTrainerDocument(trainerId, file, request);
            
            return ResponseEntity
                    .status(response.isSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST)
                    .body(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Document type không hợp lệ", null, 400));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Lỗi format dữ liệu: " + e.getMessage(), null, 400));
        }
    }

    @GetMapping("/{trainerId}")
    public ResponseEntity<ApiResponse<TrainerResponse>> getTrainerById(@PathVariable Long trainerId) {
        ApiResponse<TrainerResponse> response = trainerService.getTrainerById(trainerId);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.NOT_FOUND)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TrainerResponse>>> getAllTrainers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        ApiResponse<Page<TrainerResponse>> response = trainerService.getAllTrainers(page, size);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @GetMapping("/specialty/{specialty}")
    public ResponseEntity<ApiResponse<List<TrainerResponse>>> getTrainersBySpecialty(
            @PathVariable String specialty) {
        
        ApiResponse<List<TrainerResponse>> response = trainerService.getTrainersBySpecialty(specialty);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @PutMapping("/documents/{documentId}/verify")
    public ResponseEntity<ApiResponse<String>> verifyTrainerDocument(
            @PathVariable Long documentId,
            @RequestParam Boolean isVerified) {
        
        ApiResponse<String> response = trainerService.verifyTrainerDocument(documentId, isVerified);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @DeleteMapping("/{trainerId}/documents/{documentId}")
    public ResponseEntity<ApiResponse<String>> deleteTrainerDocument(
            @PathVariable Long trainerId,
            @PathVariable Long documentId) {
        
        ApiResponse<String> response = trainerService.deleteTrainerDocument(trainerId, documentId);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }


    @PutMapping("/{trainerId}")
    public ResponseEntity<ApiResponse<TrainerResponse>> updateTrainer(
            @PathVariable Long trainerId,
            @Valid @RequestBody CreateTrainerRequest request) {
        
        ApiResponse<TrainerResponse> response = trainerService.updateTrainer(trainerId, request);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }


    @PutMapping("/{trainerId}/deactivate")
    public ResponseEntity<ApiResponse<String>> deactivateTrainer(@PathVariable Long trainerId) {
        ApiResponse<String> response = trainerService.deactivateTrainer(trainerId);
        return ResponseEntity
                .status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }
}