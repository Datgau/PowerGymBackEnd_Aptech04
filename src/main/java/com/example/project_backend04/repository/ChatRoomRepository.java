package com.example.project_backend04.repository;

import com.example.project_backend04.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Query("""
    SELECT r.id
    FROM ChatRoom r
    JOIN ChatRoomMember m ON m.room = r
    WHERE r.isGroup = false
    AND m.user.id IN (:userAId, :userBId)
    GROUP BY r.id
    HAVING COUNT(DISTINCT m.user.id) = 2
    """)
    Optional<Long> findOneToOneRoomId(
            @Param("userAId") Long userAId,
            @Param("userBId") Long userBId
    );

    @Query("""
    SELECT DISTINCT r
    FROM ChatRoom r
    JOIN ChatRoomMember m ON m.room = r
    JOIN ChatMessage msg ON msg.room = r
    WHERE m.user.id = :userId
    """)
    List<ChatRoom> findRoomsWithMessages(@Param("userId") Long userId);

}