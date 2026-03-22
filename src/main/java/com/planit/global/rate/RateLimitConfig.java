package com.planit.global.rate;

import org.springframework.stereotype.Component;

@Component
public class RateLimitConfig {

    private static final RateLimitPolicy LOGIN_POLICY = new RateLimitPolicy(5, 60);
    private static final RateLimitPolicy CHAT_POLICY = new RateLimitPolicy(10, 60);
    private static final RateLimitPolicy DEFAULT_POLICY = new RateLimitPolicy(30, 60);

    public RateLimitPolicy resolvePolicy(String normalizedPath) {
        if (normalizedPath.startsWith("/auth/login")) {
            return LOGIN_POLICY;
        }
        if (normalizedPath.startsWith("/chat")) {
            return CHAT_POLICY;
        }
        return DEFAULT_POLICY;
    }

    public record RateLimitPolicy(int maxRequests, int windowSeconds) {
    }
}
