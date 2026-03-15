package com.example.project_backend04.dto.request.Trainer;

import com.example.project_backend04.enums.DocumentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UploadTrainerDocumentRequest {
    
    @NotNull(message = "Loại giấy tờ không được để trống")
    private DocumentType documentType;
    
    @Size(max = 200, message = "Mô tả không được quá 200 ký tự")
    private String description;
    
    private LocalDateTime expiryDate; // Ngày hết hạn (optional)
}