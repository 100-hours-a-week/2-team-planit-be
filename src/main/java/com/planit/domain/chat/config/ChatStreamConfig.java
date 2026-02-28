package com.planit.domain.chat.config;

import com.planit.domain.chat.service.ChatResultListener;
import com.planit.domain.trip.config.RedisStreamProperties;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

@Configuration
public class ChatStreamConfig {
    private static final Logger log = LoggerFactory.getLogger(ChatStreamConfig.class);
    private static final String CONSUMER_GROUP = "chat-workers";

    @Bean
    public Subscription chatResultsSubscription(
            RedisConnectionFactory connectionFactory,
            RedisStreamProperties streamProperties,
            ChatResultListener listener,
            StringRedisTemplate redisTemplate
    ) {
        ensureGroupExists(connectionFactory, streamProperties.getChatResultsKey(), CONSUMER_GROUP);

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(2))
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(connectionFactory, options);

        String consumerName = "chat-worker-" + UUID.randomUUID();

        Subscription subscription = container.receive(
                Consumer.from(CONSUMER_GROUP, consumerName),
                StreamOffset.create(streamProperties.getChatResultsKey(), ReadOffset.lastConsumed()),
                listener
        );

        container.start();
        return subscription;
    }

    private void ensureGroupExists(RedisConnectionFactory connectionFactory, String streamKey, String group) {
        try (RedisConnection connection = connectionFactory.getConnection()) {
            connection.xGroupCreate(streamKey.getBytes(), group, ReadOffset.latest(), true);
        } catch (Exception ex) {
            log.debug("Redis group create skip: stream={}, group={}, reason={}", streamKey, group, ex.getMessage());
        }
    }
}
