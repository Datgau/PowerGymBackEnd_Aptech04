package com.example.project_backend04.dto.request.Post;

import lombok.Data;

import java.util.List;

@Data
public class CreatePostRequest {
    private String content;
    private List<String> imageUrls;
}
