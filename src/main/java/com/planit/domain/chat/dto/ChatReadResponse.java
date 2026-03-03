package com.planit.domain.chat.dto;

public record ChatReadResponse(
        Long unreadCount,
        Long totalMessageCount
) {
}
