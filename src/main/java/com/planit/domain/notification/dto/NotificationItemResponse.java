package com.planit.domain.notification.dto;

import java.time.LocalDateTime;

public record NotificationItemResponse(
    Long notificationId,
    String type,
    Long postId,
    String actorName,
    String previewText,
    boolean isRead,
    LocalDateTime createdAt
) {}
