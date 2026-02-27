package com.example.project_backend04.dto.request.Story;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateStoryRequest {
    
    private MultipartFile image;
    
    private String title;
    
    private String tag; // "workout", "achievement", "motivation", "nutrition", etc.
    
    private String content;
}
