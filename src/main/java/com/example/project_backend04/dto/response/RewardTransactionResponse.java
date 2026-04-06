package com.example.project_backend04.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardTransactionResponse {
    private Long id;
    private String transactionType;
    private String transactionTypeDisplay;
    private Integer points;
    private String description;
    private LocalDateTime createdAt;
    private String formattedDate;
}
