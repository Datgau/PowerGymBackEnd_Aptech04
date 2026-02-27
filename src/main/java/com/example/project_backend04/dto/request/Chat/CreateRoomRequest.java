package com.example.project_backend04.dto.request.Chat;


import lombok.Data;

import java.util.List;

@Data
public class CreateRoomRequest {
    private String name;
    private boolean isGroup;
    private List<Long> memberIds;
}