package com.example.project_backend04.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "posts", indexes = {
        @Index(name = "idx_post_created_at", columnList = "createdAt DESC")
})
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK đến User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String images;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @ManyToMany
    @JoinTable(
            name = "post_tags",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostImage> postImages;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Like> likes;

    // ==================== Helper Methods for Images ====================
    
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Lấy danh sách URLs ảnh từ JSON string
     * @return List of image URLs, empty list if null or invalid
     */
    @Transient
    public List<String> getImageList() {
        if (images == null || images.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(images, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            System.err.println("Error parsing images JSON: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Set danh sách URLs ảnh (convert sang JSON string)
     * @param imageList List of image URLs
     */
    @Transient
    public void setImageList(List<String> imageList) {
        if (imageList == null || imageList.isEmpty()) {
            this.images = null;
            return;
        }
        try {
            this.images = objectMapper.writeValueAsString(imageList);
        } catch (JsonProcessingException e) {
            System.err.println("Error converting images to JSON: " + e.getMessage());
            this.images = null;
        }
    }

    /**
     * Thêm một ảnh vào danh sách
     * @param imageUrl URL of the image to add
     */
    @Transient
    public void addImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        
        List<String> currentImages = getImageList();
        currentImages.add(imageUrl);
        setImageList(currentImages);
    }

    /**
     * Xóa một ảnh khỏi danh sách
     * @param imageUrl URL of the image to remove
     */
    @Transient
    public void removeImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        
        List<String> currentImages = getImageList();
        currentImages.remove(imageUrl);
        setImageList(currentImages);
    }

    /**
     * Kiểm tra có ảnh không
     * @return true if has images
     */
    @Transient
    public boolean hasImages() {
        return !getImageList().isEmpty();
    }

    /**
     * Lấy số lượng ảnh
     * @return number of images
     */
    @Transient
    public int getImageCount() {
        return getImageList().size();
    }
}
