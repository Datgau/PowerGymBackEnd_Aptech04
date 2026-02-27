package com.example.project_backend04.dto.response.Notification;

import com.example.project_backend04.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private String type;
    private String content;
    private Long relatedId;
    private Boolean isRead;
    private LocalDateTime createdAt;
    
    // Actor info (người gây ra notification)
    private Long actorId;
    private String actorUsername;
    private String actorFullName;
    private String actorAvatar;

    public static NotificationDTO fromEntity(Notification notification) {
        NotificationDTOBuilder builder = NotificationDTO.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .content(notification.getContent())
                .relatedId(notification.getRelatedId())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt());

        if (notification.getActor() != null) {
            builder.actorId(notification.getActor().getId())
                   .actorUsername(notification.getActor().getUsername())
                   .actorFullName(notification.getActor().getFullName())
                   .actorAvatar(notification.getActor().getAvatar());
        }

        return builder.build();
    }
}
