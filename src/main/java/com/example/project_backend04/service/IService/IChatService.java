package com.example.project_backend04.service.IService;

import com.example.project_backend04.dto.request.Chat.ChatMessageDto;
import com.example.project_backend04.dto.request.Chat.ConversationDto;
import com.example.project_backend04.dto.request.Chat.UserSearchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.util.List;
import java.util.Optional;

public interface IChatService {
    Long getOrCreateOneToOneRoom(Long userAId, Long userBId);

    // lấy danh sách để nhắn tin
    List<UserSearchResult> getChatUsers(Long currentUserId);

    // Create group room
    Optional<Long> findOneToOneRoom(Long userAId, Long userBId);
    Long createGroupRoom(String name, List<Long> memberIds);

    // Send message
    ChatMessageDto sendMessage(Long roomId, Long senderId, String message);


    // Sử dụng đúng Pageable của Spring
    Page<ChatMessageDto> getMessages(Long roomId, Pageable pageable);

    // Fetch conversations for a user (like messenger)
    List<ConversationDto> listConversations(Long userId);

    // Mark messages as read
    void markMessagesAsRead(Long roomId, Long userId);
}
