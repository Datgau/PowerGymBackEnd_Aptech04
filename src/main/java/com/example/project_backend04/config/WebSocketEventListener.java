package com.example.project_backend04.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    @EventListener
    public void handleNotification(SessionConnectedEvent event) {
        System.out.println("Client connected!");
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        System.out.println("Client disconnected!");
    }
}

