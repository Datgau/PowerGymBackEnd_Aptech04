package com.example.project_backend04.repository;

import com.example.project_backend04.entity.ChatMessage;
import com.example.project_backend04.entity.ChatRoom;
import com.example.project_backend04.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    Page<ChatMessage> findByRoomOrderByCreatedAtDesc(ChatRoom room, Pageable pageable);

    List<ChatMessage> findTop1ByRoomOrderByCreatedAtDesc(ChatRoom room);

    long countByRoomAndIsReadFalseAndSenderNot(ChatRoom room, User sender);

    List<ChatMessage> findByRoomAndIsReadFalseAndSenderNot(ChatRoom room, User sender);
}