package com.example.project_backend04.dto.request.Chat;

/**
 * Request DTO for searching gym services
 * Used by searchGymServiceTool AI function
 */
public record SearchGymServiceRequest(
        String keyword,
        String categoryName
) {
}
