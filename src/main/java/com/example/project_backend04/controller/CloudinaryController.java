package com.example.project_backend04.controller;


import com.example.project_backend04.service.IService.ICloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/cloudinary")
public class CloudinaryController {

    @Autowired
    private ICloudinaryService cloudinaryService;

    @PostMapping("/upload-multiple")
    public ResponseEntity<List<String>> uploadFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("folderName") String folderName) {

        try {
            List<String> urls = cloudinaryService.uploadFiles(files, folderName);
            return ResponseEntity.ok(urls);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/upload-single")
    public ResponseEntity<String> uploadSingleFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("folderName") String folderName) {

        try {
            String url = cloudinaryService.uploadSingleFile(file, folderName);
            return ResponseEntity.ok(url);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Upload failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteFile(@RequestParam("url") String fileUrl) {
        try {
            cloudinaryService.deleteFile(fileUrl);
            return ResponseEntity.ok("Deleted successfully: " + fileUrl);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Delete failed: " + e.getMessage());
        }
    }
}
