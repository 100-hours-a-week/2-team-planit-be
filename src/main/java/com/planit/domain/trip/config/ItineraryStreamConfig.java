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
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

@Configuration
@ConditionalOnProperty(prefix = "app.itinerary", name = "stream-enabled", havingValue = "true", matchIfMissing = true)
public class ItineraryStreamConfig {
    private static final Logger log = LoggerFactory.getLogger(ItineraryStreamConfig.class);

    @Bean(initMethod = "start", destroyMethod = "stop")
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> itineraryResultsListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisStreamProperties streamProperties,
            ItineraryJobProperties jobProperties
    ) {
        // 1) 결과 stream + consumer group을 먼저 보장해야 XREADGROUP이 정상 동작한다.
        ensureGroupExists(connectionFactory, streamProperties.getAiResultsKey(), jobProperties.getConsumerGroup());

        // 2) container는 백그라운드에서 주기적으로 XREADGROUP(BLOCK) polling을 수행한다.
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(2))
                        .errorHandler(ex -> log.error("Redis stream listener error", ex))
                        .build();

        // 3) 이 container bean은 Spring lifecycle(start/stop)에 묶여 앱 실행 중 계속 살아있다.
        return StreamMessageListenerContainer.create(connectionFactory, options);
    }

    @Bean
    public Subscription itineraryResultsSubscription(
            StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer,
            RedisStreamProperties streamProperties,
            ItineraryJobProperties jobProperties,
            ItineraryResultListener listener
    ) {
        String consumerName = jobProperties.getConsumerName();
        if (consumerName == null || consumerName.isBlank()) {
            consumerName = "consumer-" + UUID.randomUUID();
        }

        log.info("[RESULT] subscribe stream={}, group={}, consumer={}",
                streamProperties.getAiResultsKey(), jobProperties.getConsumerGroup(), consumerName);

        // 4) receive(...) 호출 시 "구독(Subscription)"이 만들어지고,
        //    container가 poll로 가져온 메시지를 listener.onMessage(...)로 전달한다.
        return listenerContainer.receive(
                Consumer.from(jobProperties.getConsumerGroup(), consumerName),
                StreamOffset.create(streamProperties.getAiResultsKey(), ReadOffset.lastConsumed()),
                listener
        );
    }

    private void ensureGroupExists(RedisConnectionFactory connectionFactory, String streamKey, String group) {
        try (RedisConnection connection = connectionFactory.getConnection()) {
            connection.xGroupCreate(streamKey.getBytes(), group, ReadOffset.latest(), true);
        } catch (Exception ex) {
            // 이미 존재하는 경우 예외가 발생하므로 skip (최초 생성 시점만 성공하면 충분)
            log.debug("Redis group create skip: stream={}, group={}, reason={}", streamKey, group, ex.getMessage());
        }
    }
}
