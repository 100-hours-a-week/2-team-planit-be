package com.planit.domain.trip.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ItineraryJobRepository {
    private static final String KEY_PREFIX = "itinerary:job:";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_ERROR_MESSAGE = "errorMessage";
    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_UPDATED_AT = "updatedAt";

    private final StringRedisTemplate redisTemplate;

    public void initPending(Long tripId) {
        String key = buildKey(tripId);
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();
        String now = Instant.now().toString();
        ops.put(key, FIELD_STATUS, ItineraryJobStatus.PENDING.name());
        ops.put(key, FIELD_ERROR_MESSAGE, "");
        ops.put(key, FIELD_CREATED_AT, now);
        ops.put(key, FIELD_UPDATED_AT, now);
    }

    public void updateStatus(Long tripId, ItineraryJobStatus status, String errorMessage) {
        String key = buildKey(tripId);
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();
        String now = Instant.now().toString();
        ops.put(key, FIELD_STATUS, status.name());
        ops.put(key, FIELD_ERROR_MESSAGE, errorMessage == null ? "" : errorMessage);
        ops.put(key, FIELD_UPDATED_AT, now);
    }

    public Optional<Map<String, String>> findStatus(Long tripId) {
        String key = buildKey(tripId);
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();
        Map<String, String> entries = ops.entries(key);
        if (entries == null || entries.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(entries);
    }

    public void expire(Long tripId, long ttlSeconds) {
        redisTemplate.expire(buildKey(tripId), java.time.Duration.ofSeconds(ttlSeconds));
    }

    private String buildKey(Long tripId) {
        return KEY_PREFIX + tripId;
    }
}
