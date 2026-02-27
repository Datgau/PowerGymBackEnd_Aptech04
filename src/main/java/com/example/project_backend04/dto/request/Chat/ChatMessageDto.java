package com.example.project_backend04.dto.request.Chat;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageDto {
    private Long id;
    private Long roomId;
    private Long senderId;
    private String senderUsername;
    private String message;
    private boolean isRead;
    private LocalDateTime createdAt;
}
