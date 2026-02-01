package com.planit.domain.chat.service;

import com.planit.domain.chat.dto.AiChatRequest;
import com.planit.domain.chat.dto.AiChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AiChatClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final boolean mockEnabled;

    public AiChatClient(RestTemplate restTemplate,
                        @Value("${ai.base-url:http://localhost:8000}") String baseUrl,
                        @Value("${ai.mock-enabled:false}") boolean mockEnabled) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.mockEnabled = mockEnabled;
    }

    public AiChatResponse requestChat(AiChatRequest request) {
        if (mockEnabled) {
            return new AiChatResponse("일정 요청을 확인했어요. (더미 응답)");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AiChatRequest> entity = new HttpEntity<>(request, headers);
        return restTemplate.postForObject(baseUrl + "/api/v1/chat", entity, AiChatResponse.class);
    }
}
