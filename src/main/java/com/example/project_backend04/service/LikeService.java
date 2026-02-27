package com.example.project_backend04.service;

import com.example.project_backend04.dto.response.Notification.NotificationDTO;
import com.example.project_backend04.entity.*;
import com.example.project_backend04.repository.LikeRepository;
import com.example.project_backend04.repository.NotificationRepository;
import com.example.project_backend04.repository.PostRepository;
import com.example.project_backend04.repository.UserRepository;
import com.example.project_backend04.service.IService.ILikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeService implements ILikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public boolean toggleLike(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LikeId likeId = new LikeId(userId, postId);
        boolean isLiked;

        if (likeRepository.existsById(likeId)) {
            // Unlike
            likeRepository.deleteById(likeId);
            isLiked = false;
        } else {
            // Like - Dùng constructor vì Like dùng composite key
            Like like = new Like(user, post);
            likeRepository.save(like);
            isLiked = true;

            // Tạo notification nếu không phải like bài của chính mình
            if (!post.getUser().getId().equals(userId)) {
                createLikeNotification(post, user);
            }
        }

        return isLiked;
    }

    @Override
    public int getLikeCount(Long postId) {
        return likeRepository.countByPostId(postId);
    }

    @Override
    public boolean isLikedByUser(Long postId, Long userId) {
        LikeId likeId = new LikeId(userId, postId);
        return likeRepository.existsById(likeId);
    }

    private void createLikeNotification(Post post, User actor) {
        Notification notification = new Notification();
        notification.setUser(post.getUser());
        notification.setActor(actor);
        notification.setType(Notification.NotificationType.LIKE);
        notification.setContent(actor.getFullName() + " đã thích bài viết của bạn");
        notification.setRelatedId(post.getId());
        notification.setIsRead(false);

        Notification saved = notificationRepository.save(notification);

        NotificationDTO dto = NotificationDTO.fromEntity(saved);
        messagingTemplate.convertAndSendToUser(
                post.getUser().getEmail(),
                "/queue/notifications",
                dto
        );
    }
}
