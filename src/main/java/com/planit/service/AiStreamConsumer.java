package com.planit.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class AiStreamConsumer {
    private static final Logger log = LoggerFactory.getLogger(AiStreamConsumer.class);
    private final RedisConnectionFactory redisConnectionFactory;
    private final AiPlannerService plannerService;
    private final RedisTemplate<String, String> redisTemplate;

    @PostConstruct
    public void start() {
        ensureConsumerGroup();
        StreamMessageListenerContainerOptions options = StreamMessageListenerContainerOptions.builder()
                .batchSize(1)
                .pollTimeout(Duration.ofSeconds(1))
                .build();
        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(redisConnectionFactory, options);
        container.receive(
                Consumer.from("ai-workers", "travel-service"),
                StreamOffset.create("stream:ai-jobs", ReadOffset.lastConsumed()),
                message -> {
                    String recordId = message.getId().getValue();
                    String messageId = message.getValue().get("messageId");
                    String tripId = message.getValue().get("tripId");
                    log.info("Received record={} trip={} message={}", recordId, tripId, messageId);
                    plannerService.process(recordId, messageId, tripId);
                }
        );
        container.start();
    }

    private void ensureConsumerGroup() {
        try {
            redisTemplate.opsForStream().createGroup("stream:ai-jobs", "ai-workers");
        } catch (Exception ex) {
            log.debug("consumer group already exists", ex);
        }
    }
}
