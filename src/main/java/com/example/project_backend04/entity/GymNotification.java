package com.example.project_backend04.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "gym_notifications", indexes = {
        @Index(name = "idx_gym_notif_user", columnList = "user_id"),
        @Index(name = "idx_gym_notif_created", columnList = "createdAt DESC"),
        @Index(name = "idx_gym_notif_read", columnList = "isRead")
})
public class GymNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(nullable = false, length = 50)
    private String type; // SERVICE_REGISTERED, BOOKING_CONFIRMED, BOOKING_REJECTED, MEMBERSHIP_ACTIVATED

    private Long relatedId;

    @Column(length = 200)
    private String actorName;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.isRead == null) this.isRead = false;
    }
}
