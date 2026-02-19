package com.planit.domain.chat.service;

import com.planit.domain.ai.config.AiProperties;
import com.planit.domain.chat.dto.AiChatRequest;
import com.planit.domain.chat.dto.AiChatResponse;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * AI 채팅 서버와 통신하는 클라이언트.
 * @RefreshScope: Actuator의 /actuator/refresh 호출 시 재시작 없이 설정 갱신 가능
 */
@RefreshScope
@Component
public class AiChatClient {

    private final RestTemplate restTemplate;
    private final AiProperties aiProperties;

    public AiChatClient(RestTemplate restTemplate,
                        AiProperties aiProperties) {
        this.restTemplate = restTemplate;
        this.aiProperties = aiProperties;
    }

    public AiChatResponse requestChat(AiChatRequest request) {
        if (aiProperties.isMockEnabled()) {
            return new AiChatResponse("일정 요청을 확인했어요. (더미 응답)");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AiChatRequest> entity = new HttpEntity<>(request, headers);
        return restTemplate.postForObject(aiProperties.getBaseUrl() + "/api/v1/chat", entity, AiChatResponse.class);
    }
}
