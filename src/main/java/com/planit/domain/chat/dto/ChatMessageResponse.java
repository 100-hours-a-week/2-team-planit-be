package com.planit.domain.chat.dto;

import java.time.Instant;

public record ChatMessageResponse(
        String messageId,
        Long tripId,
        Long senderUserId,
        String senderNickname,
        String senderProfileImageUrl,
        String senderType,
        String content,
        Instant createdAt,
        Long seq
) {
}
