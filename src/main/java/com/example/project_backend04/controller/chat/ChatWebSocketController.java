package com.example.project_backend04.controller.chat;

import com.example.project_backend04.dto.request.Chat.ChatMessageDto;
import com.example.project_backend04.dto.request.Chat.ChatWebSocketPayload;
import com.example.project_backend04.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Client sends to /app/chat.send
     * Server persists and broadcasts to /topic/rooms/{roomId}
     */
    @MessageMapping("/chat.send")
    public void sendMessage(ChatWebSocketPayload payload) {
        ChatMessageDto saved = chatService.sendMessage(payload.getRoomId(), payload.getSenderId(), payload.getMessage());

        String topic = "/topic/rooms/" + payload.getRoomId();
        messagingTemplate.convertAndSend(topic, saved);

    }
}
