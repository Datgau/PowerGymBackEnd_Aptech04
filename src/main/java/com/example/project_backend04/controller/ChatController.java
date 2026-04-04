package com.example.project_backend04.controller;

import com.example.project_backend04.dto.request.Chat.ChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatClient chatClient;

    @PostMapping("/ask")
    public ResponseEntity<String> askAgent(@RequestBody ChatRequest request) {
        try {
            if (request.message() == null || request.message().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Message must not be empty.");
            }
            String response = chatClient.prompt()
                    .user(request.message())
                    .call()
                    .content();

            log.info("AI response generated successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            if (e.getClass().getName().contains("OpenAi")) {
                log.error("OpenAI API error: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Sorry, something went wrong. Please try again later.");
            }

            log.error("Unexpected error in chat", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Sorry, something went wrong. Please try again later.");
        }
    }
}