package com.example.project_backend04.dto.request.Chat;

/**
 * Request DTO for AI chatbot messages
 * Used by ChatController to receive user messages
 */
public record ChatRequest(String message, String sessionId) {
}
