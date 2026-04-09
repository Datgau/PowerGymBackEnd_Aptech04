package com.example.project_backend04.service;

import com.example.project_backend04.dto.response.RewardTransactionResponse;
import com.example.project_backend04.dto.response.UserRewardResponse;
import com.example.project_backend04.entity.RewardTransaction;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.entity.UserReward;
import com.example.project_backend04.enums.MembershipLevel;
import com.example.project_backend04.enums.TransactionType;
import com.example.project_backend04.repository.RewardTransactionRepository;
import com.example.project_backend04.repository.UserRewardRepository;
import com.example.project_backend04.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RewardService {
    
    private final UserRewardRepository userRewardRepository;
    private final RewardTransactionRepository rewardTransactionRepository;
    private final UserRepository userRepository;
    
    private static final int VND_PER_POINT = 1000; // 1 point = 1000 VND
    
    public UserRewardResponse getUserReward(Long userId) {
        UserReward userReward = userRewardRepository.findByUserId(userId)
            .orElseGet(() -> createUserReward(userId));
        
        return mapToResponse(userReward);
    }
    
    public List<RewardTransactionResponse> getRewardTransactions(Long userId) {
        return rewardTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(this::mapTransactionToResponse)
            .collect(Collectors.toList());
    }
    
    public Page<RewardTransactionResponse> getRewardTransactionsPaged(Long userId, Pageable pageable) {
        return rewardTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map(this::mapTransactionToResponse);
    }
    
    @Transactional
    public UserReward createUserReward(Long userId) {
        if (userRewardRepository.existsByUserId(userId)) {
            throw new RuntimeException("User reward already exists");
        }
        
        UserReward userReward = UserReward.builder()
            .userId(userId)
            .totalPoints(0)
            .membershipLevel(MembershipLevel.SILVER)
            .build();
        
        log.info("Created user reward for user: {}", userId);
        return userRewardRepository.save(userReward);
    }
    
    @Transactional
    public void earnPoints(Long userId, BigDecimal orderAmount, String paymentOrderId) {
        UserReward userReward = userRewardRepository.findByUserIdWithLock(userId)
            .orElseGet(() -> createUserReward(userId));
        int pointsEarned = orderAmount.divide(BigDecimal.valueOf(1000), 0, BigDecimal.ROUND_DOWN).intValue();
        if (pointsEarned > 0) {
            userReward.addPoints(pointsEarned);
            userRewardRepository.save(userReward);
            RewardTransaction transaction = RewardTransaction.builder()
                .userId(userId)
                .transactionType(TransactionType.EARN)
                .points(pointsEarned)
                .paymentOrderId(paymentOrderId)
                .description("Earn points from orders #" + paymentOrderId)
                .build();

            rewardTransactionRepository.save(transaction);
        }
    }
    
    @Transactional
    public BigDecimal redeemPoints(Long userId, Integer pointsToRedeem, String paymentOrderId) {
        if (pointsToRedeem == null || pointsToRedeem <= 0) {
            throw new IllegalArgumentException("Points to redeem must be positive");
        }
        
        UserReward userReward = userRewardRepository.findByUserIdWithLock(userId)
            .orElseThrow(() -> new RuntimeException("User reward not found"));
        
        if (userReward.getTotalPoints() < pointsToRedeem) {
            throw new RuntimeException("Insufficient points. Available: " + userReward.getTotalPoints());
        }
        
        userReward.deductPoints(pointsToRedeem);
        userRewardRepository.save(userReward);
        
        // Record transaction
        RewardTransaction transaction = RewardTransaction.builder()
            .userId(userId)
            .transactionType(TransactionType.REDEEM)
            .points(pointsToRedeem)
            .paymentOrderId(paymentOrderId)
            .description("Đổi " + pointsToRedeem + " điểm cho đơn hàng #" + paymentOrderId)
            .build();
        
        rewardTransactionRepository.save(transaction);
        
        BigDecimal discountAmount = BigDecimal.valueOf(pointsToRedeem * VND_PER_POINT);
        log.info("User {} redeemed {} points for {} VND", userId, pointsToRedeem, discountAmount);
        
        return discountAmount;
    }
    
    public BigDecimal calculatePointsValue(Integer points) {
        if (points == null || points <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(points * VND_PER_POINT);
    }
    
    public Integer calculatePointsFromAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return amount.divide(BigDecimal.valueOf(1000), 0, BigDecimal.ROUND_DOWN).intValue();
    }
    
    private UserRewardResponse mapToResponse(UserReward userReward) {
        MembershipLevel currentLevel = userReward.getMembershipLevel();
        String nextLevel = currentLevel == MembershipLevel.PLATINUM ? null : 
            MembershipLevel.values()[currentLevel.ordinal() + 1].name();
        
        User user = userRepository.findById(userReward.getUserId()).orElse(null);
        
        return UserRewardResponse.builder()
            .id(userReward.getId())
            .userId(userReward.getUserId())
            .userName(user != null ? user.getFullName() : null)
            .userEmail(user != null ? user.getEmail() : null)
            .userPhone(user != null ? user.getPhoneNumber() : null)
            .totalPoints(userReward.getTotalPoints())
            .membershipLevel(currentLevel.name())
            .membershipLevelDisplay(currentLevel.getDisplayName())
            .pointsToNextLevel(userReward.getPointsToNextLevel())
            .pointsValue(BigDecimal.valueOf(userReward.getTotalPoints() * VND_PER_POINT))
            .nextLevel(nextLevel)
            .build();
    }
    
    private RewardTransactionResponse mapTransactionToResponse(RewardTransaction transaction) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        return RewardTransactionResponse.builder()
            .id(transaction.getId())
            .transactionType(transaction.getTransactionType().name())
            .transactionTypeDisplay(transaction.getTransactionType().getDisplayName())
            .points(transaction.getPoints())
            .description(transaction.getDescription())
            .createdAt(transaction.getCreatedAt())
            .formattedDate(transaction.getCreatedAt().format(formatter))
            .build();
    }
}
