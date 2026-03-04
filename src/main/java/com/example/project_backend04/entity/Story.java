package com.example.project_backend04.entity;

import com.example.project_backend04.enums.StoryStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stories", indexes = {
        @Index(name = "idx_story_created_at", columnList = "createdAt DESC"),
        @Index(name = "idx_story_expires_at", columnList = "expiresAt"),
        @Index(name = "idx_story_status", columnList = "status")
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

    @Column(length = 100)
    private String title;

    @Column(length = 50)
    private String tag;

    @Column(columnDefinition = "VARCHAR(500)")
    private String content;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StoryStatus status = StoryStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    // Like and Comment counts (calculated fields) - Made nullable for backward compatibility
    @Column(name = "like_count", nullable = true)
    private Long likeCount = 0L;

    @Column(name = "comment_count", nullable = true)
    private Long commentCount = 0L;

    // Relationships for likes and comments
    @OneToMany(mappedBy = "story", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StoryLike> likes = new ArrayList<>();

    @OneToMany(mappedBy = "story", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StoryComment> comments = new ArrayList<>();

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        //Story tự động hết hạn sau 7 ngày
        this.expiresAt = this.createdAt.plusHours(168);
    }

    // Helper methods to update counts
    public void incrementLikeCount() {
        this.likeCount = (this.likeCount != null ? this.likeCount : 0L) + 1;
    }

    public void decrementLikeCount() {
        this.likeCount = this.likeCount != null ? this.likeCount : 0L;
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void incrementCommentCount() {
        this.commentCount = (this.commentCount != null ? this.commentCount : 0L) + 1;
    }

    public void decrementCommentCount() {
        this.commentCount = this.commentCount != null ? this.commentCount : 0L;
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }

    // Getters that handle null values
    public Long getLikeCount() {
        return this.likeCount != null ? this.likeCount : 0L;
    }

    public Long getCommentCount() {
        return this.commentCount != null ? this.commentCount : 0L;
    }
}
