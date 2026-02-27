package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.Chat.ChatMessageDto;
import com.example.project_backend04.dto.request.Chat.ConversationDto;
import com.example.project_backend04.dto.request.Chat.UserSearchResult;
import com.example.project_backend04.entity.*;

import com.example.project_backend04.repository.*;
import com.example.project_backend04.service.IService.IChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Service
@Transactional
@RequiredArgsConstructor
public class ChatService implements IChatService {


    private final ChatRoomRepository roomRepo;


    private final ChatRoomMemberRepository memberRepo;

    private final ChatMessageRepository messageRepo;

    private final UserRepository userRepo;

    private final FollowRepository followRepository;


    @Override
    public Long getOrCreateOneToOneRoom(Long userAId, Long userBId) {
        User a = userRepo.findById(userAId).orElseThrow();
        User b = userRepo.findById(userBId).orElseThrow();

        List<ChatRoom> candidateRooms = roomRepo.findAll().stream()
                .filter(r -> !r.isGroup())
                .collect(Collectors.toList());

        for (ChatRoom r : candidateRooms) {
            List<ChatRoomMember> members = memberRepo.findByRoom(r);
            Set<Long> ids = members.stream().map(m -> m.getUser().getId()).collect(Collectors.toSet());
            if (ids.size() == 2 && ids.contains(userAId) && ids.contains(userBId)) {
                return r.getId();
            }
        }

        ChatRoom room = new ChatRoom();
        room.setGroup(false);
        room.setName(null);
        room = roomRepo.save(room);

        ChatRoomMember m1 = new ChatRoomMember();
        m1.setRoom(room);
        m1.setUser(a);
        memberRepo.save(m1);

        ChatRoomMember m2 = new ChatRoomMember();
        m2.setRoom(room);
        m2.setUser(b);
        memberRepo.save(m2);

        return room.getId();
    }
    @Override
    public Optional<Long> findOneToOneRoom(Long userAId, Long userBId) {
        List<ChatRoom> candidateRooms = roomRepo.findAll().stream()
                .filter(r -> !r.isGroup())
                .toList();

        for (ChatRoom r : candidateRooms) {
            Set<Long> ids = memberRepo.findByRoom(r)
                    .stream()
                    .map(m -> m.getUser().getId())
                    .collect(Collectors.toSet());

            if (ids.size() == 2 && ids.contains(userAId) && ids.contains(userBId)) {
                return Optional.of(r.getId());
            }
        }
        return Optional.empty();
    }

    @Override
    public Long createGroupRoom(String name, List<Long> memberIds) {
        ChatRoom room = new ChatRoom();
        room.setGroup(true);
        room.setName(name);
        room = roomRepo.save(room);

        for (Long uid : memberIds) {
            User u = userRepo.findById(uid).orElseThrow();
            ChatRoomMember m = new ChatRoomMember();
            m.setRoom(room);
            m.setUser(u);
            memberRepo.save(m);
        }
        return room.getId();
    }

    @Override
    public ChatMessageDto sendMessage(Long roomId, Long senderId, String message) {
        ChatRoom room = roomRepo.findById(roomId).orElseThrow();
        User sender = userRepo.findById(senderId).orElseThrow();

        ChatMessage msg = new ChatMessage();
        msg.setRoom(room);
        msg.setSender(sender);
        msg.setMessage(message);
        msg.setRead(false);
        msg = messageRepo.save(msg);

        ChatMessageDto dto = toDto(msg);
        return dto;
    }

    @Override
    public Page<ChatMessageDto> getMessages(Long roomId, Pageable pageable) {
        ChatRoom room = roomRepo.findById(roomId).orElseThrow();
        Page<ChatMessage> page =
                messageRepo.findByRoomOrderByCreatedAtDesc(room, pageable);

        List<ChatMessageDto> dtos = new ArrayList<>(page.getContent().size());

        for (int i = page.getContent().size() - 1; i >= 0; i--) {
            dtos.add(toDto(page.getContent().get(i)));
        }

        return new PageImpl<>(dtos, pageable, page.getTotalElements());

    }

    @Override
    public List<ConversationDto> listConversations(Long userId) {
        User user = userRepo.findById(userId).orElseThrow();
        List<ChatRoomMember> memberships = memberRepo.findByUser(user);

        List<ConversationDto> result = new ArrayList<>();
        for (ChatRoomMember mem : memberships) {
            ChatRoom room = mem.getRoom();

            ConversationDto cd = new ConversationDto();
            cd.setRoomId(room.getId());
            cd.setGroup(room.isGroup());
            cd.setRoomName(room.getName());

            // members ids and details
            List<ChatRoomMember> roomMembers = memberRepo.findByRoom(room);
            List<Long> memberIds = roomMembers.stream()
                    .map(m -> m.getUser().getId())
                    .collect(Collectors.toList());
            cd.setMemberIds(memberIds);

            // members details
            List<UserSearchResult> members = roomMembers.stream()
                    .map(m -> {
                        User u = m.getUser();
                        return new UserSearchResult(u.getId(), u.getUsername(), u.getFullName(), u.getAvatar());
                    })
                    .collect(Collectors.toList());
            cd.setMembers(members);

            // last message
            List<ChatMessage> last = messageRepo.findTop1ByRoomOrderByCreatedAtDesc(room);
            if (!last.isEmpty()) {
                cd.setLastMessage(toDto(last.get(0)));
                cd.setLastUpdated(last.get(0).getCreatedAt());
            }

            // unread count (messages in room that isRead==false and sender != current user)
            long unread = messageRepo.countByRoomAndIsReadFalseAndSenderNot(room, user);
            cd.setUnreadCount(unread);

            result.add(cd);
        }
        result.sort((a, b) -> {
            if (a.getLastUpdated() == null && b.getLastUpdated() == null) return 0;
            if (a.getLastUpdated() == null) return 1;
            if (b.getLastUpdated() == null) return -1;
            return b.getLastUpdated().compareTo(a.getLastUpdated());
        });

        return result;
    }

    @Override
    public List<UserSearchResult> getChatUsers(Long currentUserId) {

        List<User> mutualUsers =
                followRepository.findMutualFollowUsers(currentUserId);

        return mutualUsers.stream()
                .map(u -> new UserSearchResult(
                        u.getId(),
                        u.getUsername(),
                        u.getFullName(),
                        u.getAvatar()
                ))
                .toList();
    }

    @Override
    @Transactional
    public void markMessagesAsRead(Long roomId, Long userId) {
        ChatRoom room = roomRepo.findById(roomId).orElseThrow();
        User user = userRepo.findById(userId).orElseThrow();

        // Mark all unread messages in this room (sent by others) as read
        List<ChatMessage> unreadMessages = messageRepo.findByRoomAndIsReadFalseAndSenderNot(room, user);
        for (ChatMessage msg : unreadMessages) {
            msg.setRead(true);
        }
        messageRepo.saveAll(unreadMessages);
    }

    private ChatMessageDto toDto(ChatMessage m) {
        ChatMessageDto d = new ChatMessageDto();
        d.setId(m.getId());
        d.setRoomId(m.getRoom().getId());
        d.setSenderId(m.getSender().getId());
        d.setSenderUsername(m.getSender().getUsername());
        d.setMessage(m.getMessage());
        d.setRead(m.isRead());
        d.setCreatedAt(m.getCreatedAt());
        return d;
    }
}