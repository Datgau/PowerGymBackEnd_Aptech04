package com.example.project_backend04.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "article")
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String articleId;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(length = 10000)
    private String content;

    @Column
    private String image;

    @Column
    private String author;

    @Column
    private Integer readTime;

    @ElementCollection
    @CollectionTable(name = "article_tags", joinColumns = @JoinColumn(name = "article_id"))
    @Column(name = "tag")
    private List<String> tags;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ArticleCategory category;

    @Column(nullable = false)
    private Boolean isPublished = false;

    @Column(nullable = false)
    private Boolean isFeatured = false;

    @Column
    private Integer viewCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createDate;

    @Column
    private LocalDateTime updateDate;

    @Column
    private LocalDateTime publishedDate;

    public enum ArticleCategory {
        NUTRITION,
        TRANSFORMATION,
        COMMUNITY,
        WORKOUT_TIPS,
        MENTAL_HEALTH,
        BEGINNER_GUIDE,
        ADVANCED_TRAINING,
        RECOVERY,
        EQUIPMENT_GUIDE,
        MOTIVATION,
        MEAL_PREP,
        CARDIO,
        STRENGTH,
        FLEXIBILITY,
        SUCCESS_MINDSET
    }

    @PrePersist
    protected void onCreate() {
        this.createDate = LocalDateTime.now();
        this.updateDate = LocalDateTime.now();
        if (this.articleId == null) {
            this.articleId = "ART" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateDate = LocalDateTime.now();
        if (this.isPublished && this.publishedDate == null) {
            this.publishedDate = LocalDateTime.now();
        }
    }
}