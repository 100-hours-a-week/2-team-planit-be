package com.planit.domain.chat.dto;

public record ChatMessageResponse(
        Long tripId,
        String sender,
        String message
) {
}
