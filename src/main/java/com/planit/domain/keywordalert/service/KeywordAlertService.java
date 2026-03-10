package com.planit.domain.keywordalert.service;

import com.planit.domain.keywordalert.dto.KeywordSubscriptionCreateRequest;
import com.planit.domain.keywordalert.dto.KeywordSubscriptionResponse;
import com.planit.domain.keywordalert.entity.KeywordSubscription;
import com.planit.domain.keywordalert.exception.DuplicateKeywordException;
import com.planit.domain.keywordalert.repository.KeywordSubscriptionRepository;
import com.planit.domain.notification.service.NotificationService;
import com.planit.domain.user.entity.User;
import com.planit.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class KeywordAlertService {

    private final KeywordSubscriptionRepository keywordSubscriptionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public KeywordSubscriptionResponse create(String loginId, KeywordSubscriptionCreateRequest request) {
        Long userId = resolveUserId(loginId);
        String normalized = normalizeKeyword(request.getKeyword());
        validateKeyword(normalized);
        if (keywordSubscriptionRepository.existsByUserIdAndKeyword(userId, normalized)) {
            throw new DuplicateKeywordException();
        }
        try {
            KeywordSubscription saved = keywordSubscriptionRepository.save(
                    new KeywordSubscription(userId, normalized, LocalDateTime.now()));
            return new KeywordSubscriptionResponse(saved.getId(), saved.getKeyword());
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateKeywordException();
        }
    }

    @Transactional(readOnly = true)
    public List<KeywordSubscriptionResponse> list(String loginId) {
        Long userId = resolveUserId(loginId);
        return keywordSubscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(item -> new KeywordSubscriptionResponse(item.getId(), item.getKeyword()))
                .toList();
    }

    @Transactional
    public void delete(String loginId, Long subscriptionId) {
        Long userId = resolveUserId(loginId);
        KeywordSubscription alert = keywordSubscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "*키워드를 찾을 수 없습니다."));
        keywordSubscriptionRepository.delete(alert);
    }

    @Transactional
    public void notifyMatchedKeywords(Long postId, Long actorUserId, String title, String content) {
        String safeTitle = title == null ? "" : title;
        String safeContent = content == null ? "" : content;
        if (!StringUtils.hasText(safeTitle) && !StringUtils.hasText(safeContent)) {
            return;
        }
        List<KeywordSubscription> matched = keywordSubscriptionRepository.findMatchingKeywords(safeTitle, safeContent);
        if (matched.isEmpty()) {
            return;
        }
        // 동일 게시글에서는 사용자당 1회만 생성 (여러 키워드 매칭 시 첫 키워드만 사용)
        Map<Long, KeywordSubscription> deduplicatedByUser = new LinkedHashMap<>();
        for (KeywordSubscription item : matched) {
            deduplicatedByUser.putIfAbsent(item.getUserId(), item);
        }
        for (KeywordSubscription item : deduplicatedByUser.values()) {
            if (item.getUserId().equals(actorUserId)) {
                continue;
            }
            notificationService.createKeywordNotification(item.getUserId(), postId, item.getKeyword());
        }
    }

    private Long resolveUserId(String loginId) {
        User user = userRepository.findByLoginIdAndDeletedFalse(loginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "*로그인이 필요한 요청입니다."));
        return user.getId();
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }

    private void validateKeyword(String keyword) {
        if (keyword.length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*최소 2자 이상 입력해주세요.");
        }
        if (keyword.length() > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*최대 10자까지 입력 가능합니다.");
        }
        if (keyword.matches(".*[ㄱ-ㅎㅏ-ㅣ]+.*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*초성만 입력할 수 없습니다.");
        }
        if (!keyword.matches("^[가-힣A-Za-z]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*한글/영문만 입력 가능합니다.");
        }
    }
}
