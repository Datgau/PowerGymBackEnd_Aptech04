package com.example.project_backend04.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRewardResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private String userPhone;
    private Integer totalPoints;
    private String membershipLevel;
    private String membershipLevelDisplay;
    private Integer pointsToNextLevel;
    private BigDecimal pointsValue; // VND equivalent
    private String nextLevel;
}
