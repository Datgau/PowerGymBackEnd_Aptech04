package com.example.project_backend04.dto.request.Chat;

import java.math.BigDecimal;

public record SearchMembershipRequest(
        String keyword,
        Integer duration,
        BigDecimal maxPrice
) {
}
