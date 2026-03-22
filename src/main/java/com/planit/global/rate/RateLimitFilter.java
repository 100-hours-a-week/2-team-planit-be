package com.planit.global.rate;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final String TOO_MANY_REQUESTS_MESSAGE = "Too many requests. Please try again later.";
    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimiterService rateLimiterService;
    private final RateLimitConfig rateLimitConfig;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(
            RateLimiterService rateLimiterService,
            RateLimitConfig rateLimitConfig,
            ObjectMapper objectMapper
    ) {
        this.rateLimiterService = rateLimiterService;
        this.rateLimitConfig = rateLimitConfig;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // CORS preflight, 헬스체크, 정적 리소스는 제외
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = normalizePath(request);
        return path.startsWith("/actuator")
            || path.startsWith("/swagger-ui")
            || path.startsWith("/v3/api-docs")
            || path.startsWith("/webjars")
            || path.equals("/favicon.ico")
            || path.startsWith("/static");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = normalizePath(request);
        String apiGroup = resolveApiGroup(path);
        String subjectKey = resolveSubjectKey(request);
        String redisKey = RATE_LIMIT_PREFIX + apiGroup + ":" + subjectKey;
        RateLimitConfig.RateLimitPolicy policy = rateLimitConfig.resolvePolicy(path);

        boolean allowed;
        try {
            allowed = rateLimiterService.isAllowed(redisKey, policy.maxRequests(), policy.windowSeconds());
        } catch (Exception e) {
            // 운영 안정성 우선: rate limit 저장소 장애 시 요청을 막아 전체 서비스 장애로 번지지 않게 한다.
            log.error("Rate limit check failed. key={}", redisKey, e);
            filterChain.doFilter(request, response);
            return;
        }

        if (!allowed) {
            log.warn("Rate limit exceeded. key={}", redisKey);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), Map.of("message", TOO_MANY_REQUESTS_MESSAGE));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveSubjectKey(HttpServletRequest request) {
        // 요구사항: 인증 사용자는 userId 기반, 비인증 사용자는 IP 기반 제한
        String userKey = extractUserIdFromSecurityContext();
        if (StringUtils.hasText(userKey)) {
            return "user:" + userKey;
        }
        return "ip:" + extractClientIp(request);
    }

    private String extractUserIdFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String principal = authentication.getName();
        if (!StringUtils.hasText(principal) || "anonymousUser".equals(principal)) {
            return null;
        }
        return principal;
    }

    private String extractClientIp(HttpServletRequest request) {
        // 프록시 환경 대응: X-Forwarded-For 우선 사용
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String normalizePath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();
        if (StringUtils.hasText(contextPath) && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }

    private String resolveApiGroup(String path) {
        if (path.startsWith("/auth/login")) {
            return "auth_login";
        }
        if (path.startsWith("/chat")) {
            return "chat";
        }
        if (path.startsWith("/")) {
            int next = path.indexOf('/', 1);
            return next > 0 ? path.substring(1, next) : path.substring(1);
        }
        return "default";
    }
}
