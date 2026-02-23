package com.planit.domain.trip.config;

import com.planit.domain.trip.service.ItineraryResultListener;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(prefix = "app.itinerary", name = "stream-enabled", havingValue = "true", matchIfMissing = true)
public class ItineraryStreamConfig {
    private static final Logger log = LoggerFactory.getLogger(ItineraryStreamConfig.class);

    @Bean
    public Subscription itineraryResultsSubscription(
            RedisConnectionFactory connectionFactory,
            RedisStreamProperties streamProperties,
            ItineraryJobProperties jobProperties,
            ItineraryResultListener listener,
            StringRedisTemplate redisTemplate
    ) {
        ensureGroupExists(connectionFactory, streamProperties.getAiResultsKey(), jobProperties.getConsumerGroup());

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(2))
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(connectionFactory, options);

        String consumerName = jobProperties.getConsumerName();
        if (consumerName == null || consumerName.isBlank()) {
            consumerName = "consumer-" + UUID.randomUUID();
        }

        Subscription subscription = container.receive(
                Consumer.from(jobProperties.getConsumerGroup(), consumerName),
                StreamOffset.create(streamProperties.getAiResultsKey(), ReadOffset.lastConsumed()),
                listener
        );

        container.start();
        return subscription;
    }

    private void ensureGroupExists(RedisConnectionFactory connectionFactory, String streamKey, String group) {
        try (RedisConnection connection = connectionFactory.getConnection()) {
            connection.xGroupCreate(streamKey.getBytes(), group, ReadOffset.latest(), true);
        } catch (Exception ex) {
            // 이미 존재하는 경우 예외가 발생하므로 로그만 남김
            log.debug("Redis group create skip: stream={}, group={}, reason={}", streamKey, group, ex.getMessage());
        }
    }
}
