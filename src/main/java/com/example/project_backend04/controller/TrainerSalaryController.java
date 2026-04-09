package com.example.project_backend04.controller;

import com.example.project_backend04.dto.response.TrainerSalaryResponse;
import com.example.project_backend04.service.TrainerSalaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trainers")
@RequiredArgsConstructor
public class TrainerSalaryController {
    
    private final TrainerSalaryService trainerSalaryService;
    
    @GetMapping("/{trainerId}/salary")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRAINER') and #trainerId == authentication.principal.id)")
    public ResponseEntity<TrainerSalaryResponse> getTrainerSalary(
            @PathVariable Long trainerId
    ) {
        TrainerSalaryResponse response = trainerSalaryService.calculateTotalSalary(trainerId);
        return ResponseEntity.ok(response);
    }
}
