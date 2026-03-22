package com.planit.global.rate;

import java.util.List;
import java.util.Objects;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

    /**
     * Token bucket 구현.
     * - 분산환경에서도 정확성을 보장하기 위해 Redis Lua(원자 연산) 사용
     * - 부동소수점 오차를 피하기 위해 fixed-point 스케일 사용
     */
    private static final long SCALE = 10_000L;
    private static final long REQUEST_TOKENS = 1L * SCALE;

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> tokenBucketScript;

    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.tokenBucketScript = new DefaultRedisScript<>();
        this.tokenBucketScript.setResultType(Long.class);
        this.tokenBucketScript.setScriptText("""
            local key = KEYS[1]
            local max_tokens = tonumber(ARGV[1])
            local refill_per_milli = tonumber(ARGV[2])
            local now_ms = tonumber(ARGV[3])
            local request_tokens = tonumber(ARGV[4])
            local ttl_ms = tonumber(ARGV[5])

            local bucket = redis.call('HMGET', key, 'tokens', 'last_refill_ms')
            local tokens = tonumber(bucket[1])
            local last_refill_ms = tonumber(bucket[2])

            if tokens == nil then
              tokens = max_tokens
            end
            if last_refill_ms == nil then
              last_refill_ms = now_ms
            end

            local elapsed = now_ms - last_refill_ms
            if elapsed < 0 then
              elapsed = 0
            end

            local refill = elapsed * refill_per_milli
            tokens = math.min(max_tokens, tokens + refill)

            local allowed = 0
            if tokens >= request_tokens then
              tokens = tokens - request_tokens
              allowed = 1
            end

            redis.call('HMSET', key, 'tokens', tokens, 'last_refill_ms', now_ms)
            redis.call('PEXPIRE', key, ttl_ms)

            return allowed
            """);
    }

    public boolean isAllowed(String key, int maxRequests, int windowSeconds) {
        long windowMillis = windowSeconds * 1_000L;
        long maxTokens = (long) maxRequests * SCALE;
        long refillPerMilli = Math.max(1L, maxTokens / windowMillis);

        long nowMillis = System.currentTimeMillis();
        Long result = redisTemplate.execute(
            tokenBucketScript,
            List.of(key),
            String.valueOf(maxTokens),
            String.valueOf(refillPerMilli),
            String.valueOf(nowMillis),
            String.valueOf(REQUEST_TOKENS),
            String.valueOf(windowMillis)
        );
        return Objects.equals(result, 1L);
    }
}
