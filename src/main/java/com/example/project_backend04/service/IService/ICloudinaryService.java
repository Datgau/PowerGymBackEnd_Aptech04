package com.example.project_backend04.service.IService;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ICloudinaryService {
    List<String> uploadFiles(List<MultipartFile> files, String folderName) throws IOException;
    String uploadSingleFile(MultipartFile file, String folderName) throws IOException;
    void deleteFile(String fileUrl);
}
