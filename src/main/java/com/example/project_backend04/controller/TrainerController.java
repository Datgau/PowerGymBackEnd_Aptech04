package com.example.project_backend04.controller;

import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.dto.response.Trainer.TrainerResponse;
import com.example.project_backend04.service.TrainerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trainers")
@RequiredArgsConstructor
public class TrainerController {
    
    private final TrainerService trainerService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<TrainerResponse>>> getAllActiveTrainers() {
        try {
            List<TrainerResponse> trainers = trainerService.getAllActiveTrainers();
            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    "Active trainers retrieved successfully",
                    trainers,
                    HttpStatus.OK.value()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    false,
                    e.getMessage(),
                    null,
                    HttpStatus.BAD_REQUEST.value()
            ));
        }
    }
    
    @GetMapping("/{trainerId}")
    public ResponseEntity<ApiResponse<TrainerResponse>> getTrainerById(@PathVariable Long trainerId) {
        try {
            TrainerResponse trainer = trainerService.getTrainerById(trainerId).getData();
            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    "Trainer retrieved successfully",
                    trainer,
                    HttpStatus.OK.value()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    false,
                    e.getMessage(),
                    null,
                    HttpStatus.BAD_REQUEST.value()
            ));
        }
    }
}