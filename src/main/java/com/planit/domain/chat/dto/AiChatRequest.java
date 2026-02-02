package com.planit.domain.chat.dto;

public record AiChatRequest(
        Long tripId,
        String message
) {
}
