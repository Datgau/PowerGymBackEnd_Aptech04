package com.example.project_backend04.listener;

import com.example.project_backend04.dto.response.Notification.NotificationMessage;
import com.example.project_backend04.dto.response.Service.ServiceRegistrationResponse;
import com.example.project_backend04.dto.response.User.UserResponse;
import com.example.project_backend04.event.EntityChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalNotificationListener {

    private final SimpMessagingTemplate messagingTemplate;

    @Async
    @EventListener
    public void handleEntityChange(EntityChangedEvent event) {
        log.info("Entity: {}, Action: {}", event.getEntityName(), event.getAction());
        
        String message = buildMessage(event.getEntityName(), event.getAction(), event.getData());
        Map<String, Object> notificationData = extractNotificationData(event);
        
        NotificationMessage notification = NotificationMessage.builder()
                .entityName(event.getEntityName())
                .action(event.getAction())
                .message(message)
                .data(notificationData)
                .entityId(event.getEntityId())
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/admin", notification);
        log.info("Notification sent to /topic/admin");
    }

    private String buildMessage(String entityName, String action, Object data) {
        if ("SERVICE_REGISTRATION".equals(entityName) && "REGISTERED".equals(action)) {
            String registrationType = extractRegistrationType(data);
            String serviceName = extractServiceName(data);
            if (registrationType != null && serviceName != null) {
                return "New " + registrationType + " registration for " + serviceName;
            }
        }
        
        if ("USER".equals(entityName)) {
            String userName = extractUserName(data);
            if (userName != null) {
                return switch (action) {
                    case "ACTIVATED" -> "User " + userName + " has been activated";
                    case "DEACTIVATED" -> "User " + userName + " has been deactivated";
                    case "CREATED" -> "New user " + userName + " has been created";
                    case "UPDATED" -> "User " + userName + " has been updated";
                    case "DELETED" -> "User " + userName + " has been deleted";
                    default -> "User " + userName + " " + action;
                };
            }
        }
        
        return switch (action) {
            case "CREATED" -> "New " + entityName + " has been created";
            case "UPDATED" -> entityName + " has been updated";
            case "DELETED" -> entityName + " has been deleted";
            case "REGISTERED" -> entityName + " registration successful";
            case "PAYMENT_SUCCESS" -> "Payment successful";
            case "PAYMENT_FAILED" -> "Payment failed";
            case "ACTIVATED" -> entityName + " has been activated";
            case "DEACTIVATED" -> entityName + " has been deactivated";
            default -> entityName + " " + action;
        };
    }
    
    private Map<String, Object> extractNotificationData(EntityChangedEvent event) {
        Map<String, Object> data = new HashMap<>();
        
        if (event.getData() instanceof ServiceRegistrationResponse response) {
            data.put("serviceName", response.getService().getName());
            data.put("registrationType", response.getRegistrationType() != null ? 
                response.getRegistrationType().name() : "ONLINE");
            data.put("userName", response.getUser().getFullName());
            data.put("userEmail", response.getUser().getEmail());
            data.put("registrationId", response.getId());
        } else if (event.getData() instanceof UserResponse userResponse) {
            data.put("userName", userResponse.getFullName());
            data.put("userEmail", userResponse.getEmail());
            data.put("userId", userResponse.getId());
            data.put("userRole", userResponse.getRole() != null ? userResponse.getRole().getName() : null);
            data.put("isActive", userResponse.getIsActive());
        }
        
        return data;
    }
    
    private String extractRegistrationType(Object data) {
        if (data instanceof ServiceRegistrationResponse response) {
            return response.getRegistrationType() != null ? 
                response.getRegistrationType().name() : "ONLINE";
        }
        return null;
    }
    
    private String extractServiceName(Object data) {
        if (data instanceof ServiceRegistrationResponse response) {
            return response.getService().getName();
        }
        return null;
    }
    
    private String extractUserName(Object data) {
        if (data instanceof UserResponse userResponse) {
            return userResponse.getFullName();
        }
        return null;
    }
}
