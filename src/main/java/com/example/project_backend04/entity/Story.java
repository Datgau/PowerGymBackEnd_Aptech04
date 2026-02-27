package com.example.project_backend04.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stories", indexes = {
        @Index(name = "idx_story_created_at", columnList = "createdAt DESC"),
        @Index(name = "idx_story_expires_at", columnList = "expiresAt")
})
public class Story {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String imageUrl;

    @Column(columnDefinition = "VARCHAR(500)")
    private String caption;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        // Story tự động hết hạn sau 24 giờ
        this.expiresAt = this.createdAt.plusHours(24);
    }
}
