package com.planit.domain.keywordalert.query.service;

import com.planit.domain.keywordalert.dto.KeywordSubscriptionResponse;
import com.planit.domain.keywordalert.query.repository.KeywordSubscriptionQueryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KeywordSubscriptionQueryService {

    private final KeywordSubscriptionQueryRepository queryRepository;

    public List<KeywordSubscriptionResponse> list(String loginId) {
        return queryRepository.findByLoginId(loginId)
                .stream()
                .map(row -> new KeywordSubscriptionResponse(row.getSubscriptionId(), row.getKeyword()))
                .toList();
    }
}
