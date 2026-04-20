package com.example.project_backend04.controller;

import com.example.project_backend04.dto.request.Chat.ChatRequest;
import com.example.project_backend04.dto.response.ChatResponse;
import com.example.project_backend04.service.OpenAIChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final OpenAIChatService openAIChatService;
    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> askAgent(@RequestBody ChatRequest request) {
        try {
            if (request.message() == null || request.message().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ChatResponse.textOnly("Message must not be empty."));
            }
            String sessionId = (request.sessionId() != null && !request.sessionId().isBlank())
                    ? request.sessionId()
                    : UUID.randomUUID().toString();
            ChatResponse response = openAIChatService.chat(sessionId, request.message());
            log.info("Chat OK session={} services={} memberships={} trainers={}",
                    sessionId,
                    response.services() != null ? response.services().size() : 0,
                    response.memberships() != null ? response.memberships().size() : 0,
                    response.trainers() != null ? response.trainers().size() : 0);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in chat endpoint", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ChatResponse.textOnly("Dịch vụ AI tạm thời không khả dụng. Vui lòng thử lại sau."));
        }
    }
}
