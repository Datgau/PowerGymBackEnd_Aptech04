package com.example.project_backend04.service.IService;

import com.example.project_backend04.entity.Post;
import com.example.project_backend04.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface IPostService {
    Post createPost(User user, String content, List<MultipartFile> imageFiles) throws IOException;
    
    Post createPostWithUrls(User user, String content, List<String> imageUrls);

    Post addImagesToPost(Long postId, List<MultipartFile> imageFiles) throws IOException;

    Post removeImageFromPost(Long postId, String imageUrl);

    void deletePost(Long postId);

    Post updatePostContent(Long postId, String newContent);

    Post getPostById(Long postId);

    List<Post> getPostsByUser(User user);

    List<Post> getFeedPosts(int page, int size);
}
