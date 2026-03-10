package com.planit.domain.keywordalert.service;

import com.planit.domain.keywordalert.dto.KeywordSubscriptionCreateRequest;
import com.planit.domain.keywordalert.dto.KeywordSubscriptionResponse;
import com.planit.domain.keywordalert.entity.KeywordSubscription;
import com.planit.domain.keywordalert.exception.DuplicateKeywordException;
import com.planit.domain.keywordalert.repository.KeywordSubscriptionRepository;
import com.planit.domain.user.entity.User;
import com.planit.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordSubscriptionService {

    private final KeywordSubscriptionRepository repository;
    private final UserRepository userRepository;

    @Transactional
    public KeywordSubscriptionResponse create(String loginId, KeywordSubscriptionCreateRequest request) {
        Long userId = resolveUserId(loginId);
        String keyword = normalizeKeyword(request.getKeyword());
        if (repository.existsByUserIdAndKeyword(userId, keyword)) {
            throw new DuplicateKeywordException();
        }
        try {
            KeywordSubscription saved = repository.save(new KeywordSubscription(userId, keyword, LocalDateTime.now()));
            return new KeywordSubscriptionResponse(saved.getSubscriptionId(), saved.getKeyword());
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateKeywordException();
        }
    }

    @Transactional(readOnly = true)
    public List<KeywordSubscriptionResponse> list(String loginId) {
        Long userId = resolveUserId(loginId);
        return getUserKeywords(userId);
    }

    @Transactional(readOnly = true)
    public List<KeywordSubscriptionResponse> getUserKeywords(Long userId) {
        List<KeywordSubscription> list = repository.findByUserId(userId);
        log.info("keyword subscriptions count = {}", list.size());
        return list.stream()
                .map(k -> new KeywordSubscriptionResponse(k.getSubscriptionId(), k.getKeyword()))
                .toList();
    }

    @Transactional
    public void delete(String loginId, Long subscriptionId) {
        Long userId = resolveUserId(loginId);
        KeywordSubscription target = repository.findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "*키워드를 찾을 수 없습니다."));
        repository.delete(target);
    }

    private Long resolveUserId(String loginId) {
        User user = userRepository.findByLoginIdAndDeletedFalse(loginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "*로그인이 필요한 요청입니다."));
        return user.getId();
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }
}

