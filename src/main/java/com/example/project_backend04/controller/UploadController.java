package com.example.project_backend04.controller;

import com.example.project_backend04.service.IService.ICloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final ICloudinaryService storageService;

    /**
     * Upload nhiều ảnh
     * POST /api/upload/images
     */
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImages(
            @RequestParam("images") List<MultipartFile> images,
            @RequestParam(value = "folder", defaultValue = "posts") String folder,
            Authentication authentication
    ) {
        try {
            if (images == null || images.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "No images provided");
                return ResponseEntity.badRequest().body(error);
            }

            // Upload to Cloudinary
            List<String> imageUrls = storageService.uploadFiles(images, folder);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Images uploaded successfully");
            result.put("data", Map.of("imageUrls", imageUrls));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to upload images: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Upload một ảnh
     * POST /api/upload/image
     */
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "folder", defaultValue = "general") String folder,
            Authentication authentication
    ) {
        try {
            if (image == null || image.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "No image provided");
                return ResponseEntity.badRequest().body(error);
            }

            // Upload to Cloudinary
            String imageUrl = storageService.uploadSingleFile(image, folder);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Image uploaded successfully");
            result.put("data", Map.of("imageUrl", imageUrl));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to upload image: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
