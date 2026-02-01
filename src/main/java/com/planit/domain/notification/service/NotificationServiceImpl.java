package com.planit.domain.notification.service;

import com.planit.domain.notification.dto.KeywordNotificationRequest;
import com.planit.domain.notification.dto.NotificationItemResponse;
import com.planit.domain.notification.dto.NotificationPageResponse;
import com.planit.domain.notification.dto.NotificationReadResponse;
import com.planit.domain.notification.dto.UnreadCountResponse;
import com.planit.domain.notification.entity.Notification;
import com.planit.domain.notification.entity.NotificationType;
import com.planit.domain.notification.repository.NotificationRepository;
import com.planit.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final String LIKE_PREVIEW_TEXT = "게시글을 좋아합니다";

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public NotificationPageResponse list(String loginId, Long cursor, int size, Boolean isRead) {
        Long userId = resolveUserId(loginId);
        int pageSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        Pageable pageable = PageRequest.of(0, pageSize + 1, Sort.by(Sort.Direction.DESC, "notificationId"));
        List<Notification> fetched = notificationRepository.findPage(userId, isRead, cursor, pageable);
        boolean hasNext = fetched.size() > pageSize;
        List<Notification> paged = hasNext ? fetched.subList(0, pageSize) : fetched;
        List<NotificationItemResponse> items = paged.stream().map(this::toItemResponse).toList();
        Long nextCursor = hasNext && !items.isEmpty() ? items.get(items.size() - 1).notificationId() : null;
        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);
        return new NotificationPageResponse(items, nextCursor, hasNext, unreadCount);
    }

    @Override
    @Transactional
    public NotificationReadResponse markRead(String loginId, Long notificationId) {
        Long userId = resolveUserId(loginId);
        Notification notification = notificationRepository.findById(notificationId)
            .filter(n -> n.getUserId().equals(userId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "notification not found"));
        notification.markRead(LocalDateTime.now());
        return new NotificationReadResponse(notification.getNotificationId(), notification.isRead());
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse countUnread(String loginId) {
        Long userId = resolveUserId(loginId);
        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);
        return new UnreadCountResponse(unreadCount);
    }

    @Override
    @Transactional
    public long createKeywordNotification(String loginId, KeywordNotificationRequest request) {
        Long userId = resolveUserId(loginId);
        Notification notification = Notification.builder()
            .userId(userId)
            .type(NotificationType.KEYWORD)
            .postId(request.getPostId())
            .actorName(request.getActorName())
            .previewText(request.getPreviewText())
            .build();
        notificationRepository.save(notification);
        return 1;
    }

    @Override
    @Transactional
    public void createCommentNotification(Long targetUserId, Long postId, String actorName, String previewText) {
        Notification notification = Notification.builder()
            .userId(targetUserId)
            .type(NotificationType.COMMENT)
            .postId(postId)
            .actorName(actorName)
            .previewText(previewText)
            .build();
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void createLikeNotification(Long targetUserId, Long postId, String actorName) {
        Notification notification = Notification.builder()
            .userId(targetUserId)
            .type(NotificationType.LIKE)
            .postId(postId)
            .actorName(actorName)
            .previewText(LIKE_PREVIEW_TEXT)
            .build();
        notificationRepository.save(notification);
    }

    private NotificationItemResponse toItemResponse(Notification notification) {
        return new NotificationItemResponse(
            notification.getNotificationId(),
            notification.getType().name(),
            notification.getPostId(),
            notification.getActorName(),
            notification.getPreviewText(),
            notification.isRead(),
            notification.getCreatedAt()
        );
    }

    private Long resolveUserId(String loginId) {
        return userRepository.findByLoginIdAndDeletedFalse(loginId)
            .map(user -> user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid user"));
    }
}
