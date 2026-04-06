package com.example.project_backend04.controller;

import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.dto.response.Trainer.TrainerForBookingResponse;
import com.example.project_backend04.service.TrainerBookingService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/trainers")
@RequiredArgsConstructor
public class PublicTrainerController {

    private final TrainerBookingService trainerForBookingService;


    @GetMapping("/specialty-category/{serviceId}")
    public ResponseEntity<ApiResponse<List<TrainerForBookingResponse>>> getTrainersByService(
            @PathVariable Long serviceId
    ) {
        try {
            List<TrainerForBookingResponse> trainers = trainerForBookingService.getTrainersByServiceId(serviceId);
            return ResponseEntity.ok(ApiResponse.<List<TrainerForBookingResponse>>builder()
                    .success(true)
                    .message(trainers.isEmpty() ? "No trainers found" : "Found " + trainers.size() + " trainer(s)")
                    .data(trainers)
                    .status(HttpStatus.OK.value())
                    .build());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to load trainers: " + e.getMessage()));
        }
    }
}
