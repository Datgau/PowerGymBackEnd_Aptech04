package com.example.project_backend04.service;

import com.example.project_backend04.dto.response.Notification.NotificationDTO;
import com.example.project_backend04.entity.*;
import com.example.project_backend04.repository.CommentRepository;
import com.example.project_backend04.repository.NotificationRepository;
import com.example.project_backend04.repository.PostRepository;
import com.example.project_backend04.repository.UserRepository;
import com.example.project_backend04.service.IService.ICommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService implements ICommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public Comment addComment(Long postId, Long userId, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent(content);

        Comment saved = commentRepository.save(comment);

        // Tạo notification nếu không phải comment bài của chính mình
        if (!post.getUser().getId().equals(userId)) {
            createCommentNotification(post, user, content);
        }

        return saved;
    }

    @Override
    public List<Comment> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtDesc(postId);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to delete this comment");
        }

        commentRepository.delete(comment);
    }

    private void createCommentNotification(Post post, User actor, String commentContent) {
        Notification notification = new Notification();
        notification.setUser(post.getUser());
        notification.setActor(actor);
        notification.setType(Notification.NotificationType.COMMENT);
        
        String preview = commentContent.length() > 50 
            ? commentContent.substring(0, 50) + "..." 
            : commentContent;
        notification.setContent(actor.getFullName() + " đã bình luận: \"" + preview + "\"");
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
