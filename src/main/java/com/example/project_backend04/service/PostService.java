package com.example.project_backend04.service;

import com.example.project_backend04.entity.Post;
import com.example.project_backend04.entity.PostImage;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.repository.PostRepository;
import com.example.project_backend04.service.IService.ICloudinaryService;
import com.example.project_backend04.service.IService.IPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService implements IPostService {

    private final PostRepository postRepository;
    private final ICloudinaryService storageService;

    /**
     * Tạo post mới với nhiều ảnh (tự động optimize)
     */
    @Transactional
    public Post createPost(User user, String content, List<MultipartFile> imageFiles) throws IOException {
        Post post = new Post();
        post.setUser(user);
        post.setContent(content);

        // Upload ảnh lên Cloudinary với optimization (1200x1200 cho retina)
        if (imageFiles != null && !imageFiles.isEmpty()) {
            List<String> imageUrls = new ArrayList<>();
            CloudinaryService cloudinaryService = (CloudinaryService) storageService;
            
            for (MultipartFile file : imageFiles) {
                if (file != null && !file.isEmpty()) {
                    String url = cloudinaryService.uploadPostImage(file);
                    imageUrls.add(url);
                }
            }
            
            post.setImageList(imageUrls);

            // Tạo PostImage entities
            List<PostImage> postImages = new ArrayList<>();
            for (int i = 0; i < imageUrls.size(); i++) {
                PostImage postImage = new PostImage();
                postImage.setPost(post);
                postImage.setImageUrl(imageUrls.get(i));
                postImage.setDisplayOrder(i);
                postImages.add(postImage);
            }
            post.setPostImages(postImages);
        }

        return postRepository.save(post);
    }

    /**
     * Cập nhật post - thêm ảnh mới
     */
    @Transactional
    public Post addImagesToPost(Long postId, List<MultipartFile> imageFiles) throws IOException {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (imageFiles != null && !imageFiles.isEmpty()) {
            // Upload ảnh mới
            List<String> newImageUrls = storageService.uploadFiles(imageFiles, "posts");
            
            // Thêm vào danh sách hiện tại
            List<String> currentImages = post.getImageList();
            int currentSize = currentImages.size();
            currentImages.addAll(newImageUrls);
            post.setImageList(currentImages);

            // Tạo PostImage entities cho ảnh mới
            List<PostImage> postImages = post.getPostImages();
            if (postImages == null) {
                postImages = new ArrayList<>();
            }
            
            for (int i = 0; i < newImageUrls.size(); i++) {
                PostImage postImage = new PostImage();
                postImage.setPost(post);
                postImage.setImageUrl(newImageUrls.get(i));
                postImage.setDisplayOrder(currentSize + i);
                postImages.add(postImage);
            }
            post.setPostImages(postImages);
        }

        return postRepository.save(post);
    }

    /**
     * Xóa một ảnh khỏi post
     */
    @Transactional
    public Post removeImageFromPost(Long postId, String imageUrl) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        // Xóa ảnh khỏi Cloudinary
        storageService.deleteFile(imageUrl);

        // Xóa khỏi JSON array
        post.removeImage(imageUrl);

        // Xóa PostImage entity
        if (post.getPostImages() != null) {
            post.getPostImages().removeIf(img -> img.getImageUrl().equals(imageUrl));
        }

        return postRepository.save(post);
    }

    /**
     * Xóa post và tất cả ảnh
     */
    @Transactional
    public void deletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        // Xóa tất cả ảnh khỏi Cloudinary
        List<String> imageUrls = post.getImageList();
        for (String imageUrl : imageUrls) {
            storageService.deleteFile(imageUrl);
        }

        // Xóa post (cascade sẽ xóa PostImage, Comment, Like)
        postRepository.delete(post);
    }

    /**
     * Cập nhật nội dung post
     */
    @Transactional
    public Post updatePostContent(Long postId, String newContent) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        post.setContent(newContent);
        return postRepository.save(post);
    }

    /**
     * Lấy post theo ID với eager loading
     */
    public Post getPostById(Long postId) {
        return postRepository.findByIdWithDetails(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
    }

    /**
     * Lấy tất cả posts của user
     */
    public List<Post> getPostsByUser(User user) {
        return postRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Tạo post với URLs ảnh đã upload sẵn (không upload ảnh)
     */
    @Transactional
    public Post createPostWithUrls(User user, String content, List<String> imageUrls) {
        Post post = new Post();
        post.setUser(user);
        post.setContent(content);

        if (imageUrls != null && !imageUrls.isEmpty()) {
            post.setImageList(imageUrls);

            // Tạo PostImage entities
            List<PostImage> postImages = new ArrayList<>();
            for (int i = 0; i < imageUrls.size(); i++) {
                PostImage postImage = new PostImage();
                postImage.setPost(post);
                postImage.setImageUrl(imageUrls.get(i));
                postImage.setDisplayOrder(i);
                postImages.add(postImage);
            }
            post.setPostImages(postImages);
        }

        return postRepository.save(post);
    }

    /**
     * Lấy feed posts (tất cả posts, sắp xếp theo thời gian)
     * TODO: Implement pagination và logic feed (following users, etc.)
     */
    public List<Post> getFeedPosts(int page, int size) {
        List<Post> allPosts = postRepository.findAllByOrderByCreatedAtDesc();
        System.out.println("Total posts found: " + allPosts.size());
        return allPosts;
    }
}
