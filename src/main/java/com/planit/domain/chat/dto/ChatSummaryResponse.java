package com.planit.domain.chat.dto;

public record ChatSummaryResponse(
        Long unreadCount,
        Long totalMessageCount
) {
}
