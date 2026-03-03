package com.example.project_backend04.dto.response.Notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    private String entityName;
    private String action;
    private String message;
    private Object data;
    private Long entityId;
    private LocalDateTime timestamp;
}
