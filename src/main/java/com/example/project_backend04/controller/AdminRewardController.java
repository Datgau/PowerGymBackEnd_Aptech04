package com.example.project_backend04.controller;

import com.example.project_backend04.dto.response.RewardTransactionResponse;
import com.example.project_backend04.dto.response.UserRewardResponse;
import com.example.project_backend04.entity.RewardTransaction;
import com.example.project_backend04.entity.UserReward;
import com.example.project_backend04.repository.RewardTransactionRepository;
import com.example.project_backend04.repository.UserRewardRepository;
import com.example.project_backend04.service.RewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/rewards")
@RequiredArgsConstructor
public class AdminRewardController {
    
    private final UserRewardRepository userRewardRepository;
    private final RewardTransactionRepository rewardTransactionRepository;
    private final RewardService rewardService;
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUserRewards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("totalPoints").descending());
        Page<UserReward> userRewardsPage = userRewardRepository.findAll(pageable);
        
        Page<UserRewardResponse> responsePage = userRewardsPage.map(userReward -> 
            rewardService.getUserReward(userReward.getUserId())
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", responsePage.getContent());
        response.put("currentPage", responsePage.getNumber());
        response.put("totalPages", responsePage.getTotalPages());
        response.put("totalItems", responsePage.getTotalElements());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserRewardByUserId(@PathVariable Long userId) {
        UserRewardResponse reward = rewardService.getUserReward(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", reward);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user/{userId}/transactions")
    public ResponseEntity<Map<String, Object>> getUserTransactions(@PathVariable Long userId) {
        List<RewardTransactionResponse> transactions = rewardService.getRewardTransactions(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", transactions);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/transactions")
    public ResponseEntity<Map<String, Object>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<RewardTransaction> transactionsPage = rewardTransactionRepository.findAll(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", transactionsPage.getContent());
        response.put("currentPage", transactionsPage.getNumber());
        response.put("totalPages", transactionsPage.getTotalPages());
        response.put("totalItems", transactionsPage.getTotalElements());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getRewardStatistics() {
        List<UserReward> allRewards = userRewardRepository.findAll();
        
        long totalUsers = allRewards.size();
        int totalPoints = allRewards.stream()
                .mapToInt(UserReward::getTotalPoints)
                .sum();
        
        long silverCount = allRewards.stream()
                .filter(r -> r.getMembershipLevel().name().equals("SILVER"))
                .count();
        long goldCount = allRewards.stream()
                .filter(r -> r.getMembershipLevel().name().equals("GOLD"))
                .count();
        long platinumCount = allRewards.stream()
                .filter(r -> r.getMembershipLevel().name().equals("PLATINUM"))
                .count();
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalUsers", totalUsers);
        statistics.put("totalPoints", totalPoints);
        statistics.put("averagePoints", totalUsers > 0 ? totalPoints / totalUsers : 0);
        statistics.put("membershipDistribution", Map.of(
                "SILVER", silverCount,
                "GOLD", goldCount,
                "PLATINUM", platinumCount
        ));
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", statistics);
        
        return ResponseEntity.ok(response);
    }
}
