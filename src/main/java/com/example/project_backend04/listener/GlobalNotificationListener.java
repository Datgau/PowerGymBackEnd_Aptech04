package com.example.project_backend04.listener;

import com.example.project_backend04.dto.response.Notification.NotificationMessage;
import com.example.project_backend04.event.EntityChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalNotificationListener {

    private final SimpMessagingTemplate messagingTemplate;

    @Async
    @EventListener
    public void handleEntityChange(EntityChangedEvent event) {
        log.info("=== Entity Changed Event ===");
        log.info("Entity: {}, Action: {}", event.getEntityName(), event.getAction());
        
        String message = buildMessage(event.getEntityName(), event.getAction());
        
        NotificationMessage notification = NotificationMessage.builder()
                .entityName(event.getEntityName())
                .action(event.getAction())
                .message(message)
                .data(event.getData())
                .entityId(event.getEntityId())
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/global", notification);
        log.info("Notification sent to /topic/global");
        
        String entityTopic = "/topic/" + event.getEntityName().toLowerCase();
        messagingTemplate.convertAndSend(entityTopic, notification);
        log.info("Notification sent to {}", entityTopic);
    }

    private String buildMessage(String entityName, String action) {
        return switch (action) {
            case "CREATED" -> entityName + " mới đã được tạo";
            case "UPDATED" -> entityName + " đã được cập nhật";
            case "DELETED" -> entityName + " đã được xóa";
            case "REGISTERED" -> "Đăng ký " + entityName + " thành công";
            case "PAYMENT_SUCCESS" -> "Thanh toán thành công";
            case "PAYMENT_FAILED" -> "Thanh toán thất bại";
            default -> entityName + " " + action;
        };
    }
}
