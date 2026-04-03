package com.example.project_backend04.entity;

import com.example.project_backend04.enums.MediaType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "media")
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // URL ảnh sau khi upload (S3, Cloudinary, local...)
    private String url;

    // Tên file gốc
    private String fileName;

    // Loại ảnh: BANNER, ABOUT, HOME, GALLERY...
    @Enumerated(EnumType.STRING)
    private MediaType type;

    // Mô tả (optional)
    private String description;

    // Thứ tự hiển thị
    private Integer displayOrder;

    private Boolean isActive;
    private String redirectUrl;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
