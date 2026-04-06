package com.example.project_backend04.controller;

import com.example.project_backend04.dto.response.RewardTransactionResponse;
import com.example.project_backend04.dto.response.UserRewardResponse;
import com.example.project_backend04.security.CustomUserDetails;
import com.example.project_backend04.service.RewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rewards")
@RequiredArgsConstructor
public class RewardController {
    
    private final RewardService rewardService;
    
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getMyReward(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Long userId = userDetails.getId();
        UserRewardResponse reward = rewardService.getUserReward(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", reward);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/my-rewards")
    public ResponseEntity<Map<String, Object>> getMyRewards(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Long userId = userDetails.getId();
        UserRewardResponse reward = rewardService.getUserReward(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", reward);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/transactions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getMyTransactions(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Long userId = userDetails.getId();
        List<RewardTransactionResponse> transactions = rewardService.getRewardTransactions(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", transactions);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUserReward(@PathVariable Long userId) {
        UserRewardResponse reward = rewardService.getUserReward(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", reward);
        
        return ResponseEntity.ok(response);
    }
}
