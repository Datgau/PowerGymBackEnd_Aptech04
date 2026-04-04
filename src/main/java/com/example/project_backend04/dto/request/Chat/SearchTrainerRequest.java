package com.example.project_backend04.dto.request.Chat;

/**
 * Request DTO for searching trainers
 * Used by searchTrainerTool AI function
 */
public record SearchTrainerRequest(
        String specialtyName  // Optional: tên chuyên môn (VD: "Yoga", "Boxing", "Weight Training")
) {
}
