package com.example.project_backend04.mapper;


import com.example.project_backend04.dto.response.Story.StoryResponseDto;
import com.example.project_backend04.entity.Story;
import com.example.project_backend04.entity.User;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class StoriesMapper {

    public StoryResponseDto toDto(Story story) {
        User user = story.getUser();
        LocalDateTime now = LocalDateTime.now();

        return StoryResponseDto.builder()
                .id(story.getId())
                .imageUrl(story.getImageUrl())
                .title(story.getTitle())
                .tag(story.getTag())
                .content(story.getContent())
                .isActive(story.getIsActive())
                .status(story.getStatus().name())
                .createdAt(story.getCreatedAt())
                .expiresAt(story.getExpiresAt())
                .approvedAt(story.getApprovedAt())
                .userId(user.getId())
                .username(user.getEmail()) // bạn đang dùng email làm username
                .userAvatar(user.getAvatar())
                .userFullName(user.getFullName())
                .isExpired(story.getExpiresAt().isBefore(now))
                .timeAgo(getTimeAgo(story.getCreatedAt()))
                .build();
    }

    private String getTimeAgo(LocalDateTime createdAt) {
        Duration duration = Duration.between(createdAt, LocalDateTime.now());

        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + " seconds ago";
        }

        long minutes = duration.toMinutes();
        if (minutes < 60) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        }

        long hours = duration.toHours();
        if (hours < 24) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        }

        long days = duration.toDays();
        return days + " day" + (days > 1 ? "s" : "") + " ago";
    }
}