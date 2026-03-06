package com.planit.domain.post.stats.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(StringRedisTemplate.class)
public class PostStatsCacheService {

    private static final String LIKE_KEY_PREFIX = "post:stats:like:";
    private static final String COMMENT_KEY_PREFIX = "post:stats:comment:";
    private static final String VIEW_KEY_PREFIX = "post:stats:view:";

    private final StringRedisTemplate redisTemplate;

    public PostStatsCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public long increaseLike(Long postId) {
        Long value = redisTemplate.opsForValue().increment(LIKE_KEY_PREFIX + postId);
        return value == null ? 0L : value;
    }

    public long increaseComment(Long postId) {
        Long value = redisTemplate.opsForValue().increment(COMMENT_KEY_PREFIX + postId);
        return value == null ? 0L : value;
    }

    public long increaseView(Long postId) {
        Long value = redisTemplate.opsForValue().increment(VIEW_KEY_PREFIX + postId);
        return value == null ? 0L : value;
    }
}
