package com.example.project_backend04.dto.request.Chat;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ConversationDto {
    private Long roomId;
    private boolean isGroup;
    private String roomName; // nếu có
    private List<Long> memberIds;
    private List<UserSearchResult> members; // Thông tin chi tiết của members
    private ChatMessageDto lastMessage;
    private long unreadCount;
    private LocalDateTime lastUpdated;
}