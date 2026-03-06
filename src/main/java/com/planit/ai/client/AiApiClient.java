package com.planit.ai.client;

import com.planit.ai.dto.AiRequest;
import com.planit.ai.dto.AiResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class AiApiClient {
    private static final Logger log = LoggerFactory.getLogger(AiApiClient.class);
    private final WebClient aiWebClient;
    @Value("${ai.mock-enabled:false}")
    private boolean mockEnabled;

    public String requestAiReply(AiRequest request) {
        if (mockEnabled) {
            String reply = "Mock AI response for tripId=" + request.getTripId();
            log.info("Returning mock AI reply for tripId={}", request.getTripId());
            return reply;
        }

        AiResponse response = aiWebClient.post()
                .uri("/api/v1/chatbot")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiResponse.class)
                .block();
        if (response == null) {
            log.warn("AI response empty for tripId={}", request.getTripId());
            throw new IllegalStateException("AI service returned empty response");
        }
        System.out.println("AI챗봇 응답 객체: "+response);
        return response.getContent();
    }
}
