package com.planit.domain.notification.service;

import com.planit.domain.notification.dto.CreatedCountResponse;
import com.planit.domain.notification.dto.KeywordNotificationRequest;
import com.planit.domain.notification.dto.NotificationPageResponse;
import com.planit.domain.notification.dto.NotificationReadResponse;
import com.planit.domain.notification.dto.UnreadCountResponse;

public interface NotificationService {

    NotificationPageResponse list(String loginId, Long cursor, int size, Boolean isRead);

    NotificationReadResponse markRead(String loginId, Long notificationId);

    UnreadCountResponse countUnread(String loginId);

    long createKeywordNotification(String loginId, KeywordNotificationRequest request);

    void createCommentNotification(Long targetUserId, Long postId, String actorName, String previewText);

    void createLikeNotification(Long targetUserId, Long postId, String actorName);
}
