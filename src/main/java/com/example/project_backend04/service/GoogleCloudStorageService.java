package com.example.project_backend04.service;

import com.example.project_backend04.service.IService.IGoogleCloudStorageService;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "google.cloud.enabled", 
    havingValue = "true", 
    matchIfMissing = false
)
public class GoogleCloudStorageService implements IGoogleCloudStorageService {

    @Value("${google.cloud.bucket-name}")
    private String bucketName;

    @Value("${google.cloud.credential-path:}")
    private String credentialPath;

    @Value("${google.cloud.credentials-json:}")
    private String credentialsJson;

    private Storage storage;

    @PostConstruct
    private void init() throws IOException {
        GoogleCredentials credentials;
        
        // Ưu tiên dùng JSON string từ environment variable (cho Render/Cloud)
        if (credentialsJson != null && !credentialsJson.isBlank()) {
            credentials = GoogleCredentials.fromStream(
                new java.io.ByteArrayInputStream(credentialsJson.getBytes())
            );
        } 
        // Fallback: dùng file path (cho local development)
        else if (credentialPath != null && !credentialPath.isBlank()) {
            String absPath = Paths.get(credentialPath).toAbsolutePath().toString();
            credentials = GoogleCredentials.fromStream(new FileInputStream(absPath));
        } 
        else {
            throw new IllegalStateException(
                "Google Cloud credentials not configured. " +
                "Set either 'google.cloud.credentials-json' or 'google.cloud.credential-path'"
            );
        }
        
        storage = StorageOptions.newBuilder()
                .setCredentials(credentials)
                .build()
                .getService();
    }

    public List<String> uploadFiles(List<MultipartFile> files, String folderName) throws IOException {
        List<String> uploadedUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            uploadedUrls.add(uploadSingleFile(file, folderName));
        }

        return uploadedUrls;
    }

    public String uploadSingleFile(MultipartFile file, String folderName) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Invalid file");
        }

        String objectName = folderName + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();

        storage.create(blobInfo, file.getBytes());

        return String.format("https://storage.googleapis.com/%s/%s", bucketName, objectName);
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;

        try {
            String objectName = fileUrl.substring(fileUrl.indexOf(bucketName) + bucketName.length() + 1);

            boolean deleted = storage.delete(BlobId.of(bucketName, objectName));

            if (!deleted) {
                System.out.println("[Warning] File not found on Google Cloud Storage: " + fileUrl);
            } else {
                System.out.println("[Info] Deleted: gs://" + bucketName + "/" + objectName);
            }
        } catch (Exception ex) {
            System.err.println("[Error] Failed to delete file: " + ex.getMessage());
        }
    }

}
