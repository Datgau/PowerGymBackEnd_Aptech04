package com.example.project_backend04.entity;

import com.example.project_backend04.enums.MembershipLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_rewards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserReward {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
    
    @Column(name = "total_points", nullable = false)
    @Builder.Default
    private Integer totalPoints = 0;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "membership_level", nullable = false)
    @Builder.Default
    private MembershipLevel membershipLevel = MembershipLevel.SILVER;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        updateMembershipLevel();
    }
    
    public void addPoints(int points) {
        if (points < 0) {
            throw new IllegalArgumentException("Points to add must be positive");
        }
        this.totalPoints += points;
        updateMembershipLevel();
    }
    
    public void deductPoints(int points) {
        if (points < 0) {
            throw new IllegalArgumentException("Points to deduct must be positive");
        }
        if (this.totalPoints < points) {
            throw new IllegalArgumentException("Insufficient points");
        }
        this.totalPoints -= points;
        updateMembershipLevel();
    }
    
    private void updateMembershipLevel() {
        this.membershipLevel = MembershipLevel.fromPoints(this.totalPoints);
    }
    
    public int getPointsToNextLevel() {
        if (membershipLevel == MembershipLevel.PLATINUM) {
            return 0;
        }
        return membershipLevel.getPointsToNext() - totalPoints;
    }
}
