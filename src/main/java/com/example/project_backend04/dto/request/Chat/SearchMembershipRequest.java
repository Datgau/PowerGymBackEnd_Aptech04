package com.example.project_backend04.dto.request.Chat;

import java.math.BigDecimal;

/**
 * Request DTO for searching membership packages
 * Used by searchMembershipPackagesTool AI function
 */
public record SearchMembershipRequest(
        String keyword,
        Integer duration,
        BigDecimal maxPrice
) {
}
