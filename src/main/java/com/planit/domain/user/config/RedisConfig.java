package com.planit.domain.user.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    // 인증 토큰은 문자열 키/값이므로 StringRedisTemplate이 가장 단순하고 빠르다.
    // Redis를 토큰 저장소로 사용하면 DB I/O보다 가볍고 TTL 만료를 저장소 레벨에서 일관되게 처리할 수 있다.
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6380}") int port,
            @Value("${spring.data.redis.password:}") String password
    ) {
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(host, port);
        if (password != null && !password.isBlank()) {
            standalone.setPassword(RedisPassword.of(password));
        }
        LettuceConnectionFactory factory = new LettuceConnectionFactory(standalone);
        log.info("RedisConnectionFactory configured - host={}, port={}", host, port);
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean(StringRedisTemplate.class)
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());

        String factoryType = connectionFactory.getClass().getName();
        log.info("StringRedisTemplate uses ConnectionFactory={}", factoryType);

        try (RedisConnection connection = connectionFactory.getConnection()) {
            String pong = connection.ping();
            log.info("Redis connection check - ping={}, factory={}", pong, factoryType);
        } catch (Exception e) {
            log.error("Redis connection check failed - factory={}", factoryType, e);
        }
        return template;
    }
}
