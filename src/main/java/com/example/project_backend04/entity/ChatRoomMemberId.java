package com.example.project_backend04.entity;

import lombok.*;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomMemberId implements Serializable {
    private Long room;
    private Long user;
}
