package com.example.project_backend04.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.example.project_backend04.service.IService.ICloudinaryService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudinaryService implements ICloudinaryService {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    private Cloudinary cloudinary;

    @PostConstruct
    private void init() {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    @Override
    public List<String> uploadFiles(List<MultipartFile> files, String folderName) throws IOException {
        List<String> uploadedUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            uploadedUrls.add(uploadSingleFile(file, folderName));
        }

        return uploadedUrls;
    }

    @Override
    public String uploadSingleFile(MultipartFile file, String folderName) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Invalid file");
        }
        String publicId = folderName + "/" + UUID.randomUUID() + "_" + 
                          file.getOriginalFilename().replaceAll("\\s+", "_");

        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), 
            ObjectUtils.asMap(
                "public_id", publicId,
                "folder", folderName,
                "resource_type", "auto",
                "quality", "auto:good",
                "fetch_format", "auto",
                "flags", "progressive"
            )
        );

        return (String) uploadResult.get("secure_url");
    }

    public String uploadWithTransformation(MultipartFile file, String folderName, 
                                          int width, int height, String cropMode) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Invalid file");
        }

        String publicId = folderName + "/" + UUID.randomUUID() + "_" + 
                          file.getOriginalFilename().replaceAll("\\s+", "_");

        // Create transformation
        Transformation transformation = new Transformation()
            .width(width)
            .height(height)
            .crop(cropMode) // "fill", "fit", "scale", "thumb"
            .quality("auto:good")
            .fetchFormat("auto");

        // Upload with eager transformation (resize ngay khi upload)
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), 
            ObjectUtils.asMap(
                "public_id", publicId,
                "folder", folderName,
                "resource_type", "auto",
                "quality", "auto:good",
                "fetch_format", "auto",
                "eager", List.of(transformation) // ✅ Wrap trong List
            )
        );

        return (String) uploadResult.get("secure_url");
    }

    public String uploadStory(MultipartFile file) throws IOException {
        return uploadWithTransformation(file, "stories", 1080, 1920, "fill");
    }

    public String uploadServiceImage(MultipartFile file) throws IOException {
        return uploadWithTransformation(file, "services", 1600, 900, "fill");
    }

    public String getOptimizedUrl(String originalUrl, int width, int height, String cropMode) {
        try {
            String publicId = extractPublicIdFromUrl(originalUrl);
            if (publicId == null) return originalUrl;
            return cloudinary.url()
                .transformation(new Transformation()
                    .width(width)
                    .height(height)
                    .crop(cropMode)
                    .quality("auto:good")
                    .fetchFormat("auto"))
                .generate(publicId);
        } catch (Exception e) {
            System.err.println("[Error] Failed to generate optimized URL: " + e.getMessage());
            return originalUrl;
        }
    }
    public Map<String, String> getResponsiveUrls(String originalUrl) {
        String publicId = extractPublicIdFromUrl(originalUrl);
        if (publicId == null) return Map.of("original", originalUrl);

        return Map.of(
            "thumbnail", getOptimizedUrl(originalUrl, 300, 300, "fill"),    // 150x150 display
            "small", getOptimizedUrl(originalUrl, 600, 600, "fill"),        // 300x300 display
            "medium", getOptimizedUrl(originalUrl, 1200, 1200, "fill"),     // 600x600 display
            "large", getOptimizedUrl(originalUrl, 2400, 2400, "fill"),      // 1200x1200 display
            "original", originalUrl
        );
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;

        try {
            String publicId = extractPublicIdFromUrl(fileUrl);
            
            if (publicId != null && !publicId.isBlank()) {
                Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                
                String resultStatus = (String) result.get("result");
                if ("ok".equals(resultStatus)) {
                    System.out.println("[Info] Deleted from Cloudinary: " + publicId);
                } else {
                    System.out.println("[Warning] File not found on Cloudinary: " + publicId);
                }
            }
        } catch (Exception ex) {
            System.err.println("[Error] Failed to delete file from Cloudinary: " + ex.getMessage());
        }
    }

    private String extractPublicIdFromUrl(String url) {
        try {
            int uploadIndex = url.indexOf("/upload/");
            if (uploadIndex == -1) return null;
            String afterUpload = url.substring(uploadIndex + 8); // 8 = length of "/upload/"
            int slashIndex = afterUpload.indexOf('/');
            if (slashIndex == -1) return null;
            
            String pathWithExtension = afterUpload.substring(slashIndex + 1);
            int lastDotIndex = pathWithExtension.lastIndexOf('.');
            if (lastDotIndex != -1) {
                return pathWithExtension.substring(0, lastDotIndex);
            }
            
            return pathWithExtension;
        } catch (Exception e) {
            System.err.println("[Error] Failed to extract public_id from URL: " + url);
            return null;
        }
    }
}
