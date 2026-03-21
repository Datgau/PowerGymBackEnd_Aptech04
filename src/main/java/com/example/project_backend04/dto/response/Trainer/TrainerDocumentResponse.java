package com.example.project_backend04.dto.response.Trainer;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TrainerDocumentResponse {
    private Long id;
    private String documentType;
    private String fileName;
    private String fileUrl;
    private String description;
    private LocalDateTime expiryDate;
    private Boolean isVerified;
    private Boolean isActive;
    private LocalDateTime createdAt;
}