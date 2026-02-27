package com.example.project_backend04.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@IdClass(ChatRoomMemberId.class)
@Table(name = "chat_room_members")
public class ChatRoomMember {

    @Id
    @ManyToOne
    @JoinColumn(name = "room_id")
    private ChatRoom room;

    @Id
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDateTime joinedAt;

    @PrePersist
    void onJoin() {
        this.joinedAt = LocalDateTime.now();
    }
}
