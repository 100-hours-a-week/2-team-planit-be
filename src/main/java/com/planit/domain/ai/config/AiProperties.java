package com.planit.domain.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 서버 설정을 관리하는 Properties 클래스.
 * Actuator의 /actuator/refresh 엔드포인트 호출 시 재시작 없이 설정 갱신 가능
 * (AiItineraryClient, AiChatClient에 @RefreshScope 적용)
 */
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /**
     * AI 서버의 base URL.
     * v1: 단일 인스턴스 → localhost:8000
     * v2: AI 전용 인스턴스 → AI_BASE_URL 환경변수 또는 외부 설정 파일로 설정
     */
    private String baseUrl = "http://localhost:8000";

    /**
     * Mock 모드 활성화 여부 (테스트용)
     */
    private boolean mockEnabled = false;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isMockEnabled() {
        return mockEnabled;
    }

    public void setMockEnabled(boolean mockEnabled) {
        this.mockEnabled = mockEnabled;
    }
}
