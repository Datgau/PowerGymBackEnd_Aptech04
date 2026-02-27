package com.example.project_backend04.dto.request.Chat;
import lombok.Data;

@Data
public class SendMessageRequest {
    private Long roomId;
    private Long senderId;
    private String message;
}
