package com.planit.domain.notification.controller;

import com.planit.domain.notification.dto.CreatedCountResponse;
import com.planit.domain.notification.dto.KeywordNotificationRequest;
import com.planit.domain.notification.dto.NotificationPageResponse;
import com.planit.domain.notification.dto.NotificationReadResponse;
import com.planit.domain.notification.dto.UnreadCountResponse;
import com.planit.domain.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public NotificationPageResponse list(@AuthenticationPrincipal UserDetails principal,
                                         @RequestParam(required = false) Long cursor,
                                         @RequestParam(defaultValue = "20") int size,
                                         @RequestParam(required = false) Boolean isRead) {
        return notificationService.list(requireLogin(principal), cursor, size, isRead);
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationReadResponse markRead(@AuthenticationPrincipal UserDetails principal,
                                             @PathVariable Long notificationId) {
        return notificationService.markRead(requireLogin(principal), notificationId);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal UserDetails principal) {
        return notificationService.countUnread(requireLogin(principal));
    }

    @PostMapping("/keyword")
    public ResponseEntity<CreatedCountResponse> createKeyword(@AuthenticationPrincipal UserDetails principal,
                                                              @Valid @RequestBody KeywordNotificationRequest request) {
        long created = notificationService.createKeywordNotification(requireLogin(principal), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreatedCountResponse(created));
    }

    private String requireLogin(UserDetails principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "*로그인이 필요한 요청입니다.");
        }
        return principal.getUsername();
    }
}
