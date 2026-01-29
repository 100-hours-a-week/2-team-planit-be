package com.planit.domain.notification.dto;

import java.util.List;

public record NotificationPageResponse(
    List<NotificationItemResponse> items,
    Long nextCursor,
    boolean hasNext,
    long unreadCount
) {}
