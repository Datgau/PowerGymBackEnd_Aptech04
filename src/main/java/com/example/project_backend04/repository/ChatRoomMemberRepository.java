package com.example.project_backend04.repository;

import com.example.project_backend04.entity.ChatRoom;
import com.example.project_backend04.entity.ChatRoomMember;
import com.example.project_backend04.entity.ChatRoomMemberId;
import com.example.project_backend04.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, ChatRoomMemberId> {
    Optional<ChatRoomMember> findByRoomAndUser(ChatRoom room, User user);

    List<ChatRoomMember> findByUser(User user);

    List<ChatRoomMember> findByRoom(ChatRoom room);

    boolean existsByRoomAndUser(ChatRoom room, User user);
}