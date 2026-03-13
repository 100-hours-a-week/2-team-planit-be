package com.planit.domain.trip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planit.domain.trip.config.ItineraryJobProperties;
import com.planit.domain.trip.config.RedisStreamProperties;
import com.planit.domain.trip.dto.AiItineraryResponse;
import com.planit.domain.trip.entity.Trip;
import com.planit.domain.trip.entity.TripStatus;
import com.planit.domain.trip.repository.TripRepository;
import java.util.Map;

import com.planit.domain.trip.service.redisAccessor.ItineraryJobService;
import com.planit.domain.trip.service.redisAccessor.ItineraryJobStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class ItineraryResultListener implements StreamListener<String, MapRecord<String, String, String>> {
    private static final Logger log = LoggerFactory.getLogger(ItineraryResultListener.class);

    private final ObjectMapper objectMapper;
    private final ItineraryJobService jobService;
    private final AiItineraryProcessor processor;
    private final StringRedisTemplate redisTemplate;
    private final RedisStreamProperties streamProperties;
    private final ItineraryJobProperties jobProperties;
    private final TripRepository tripRepository;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        // 5) container가 poll로 가져온 각 record를 이 콜백으로 전달한다.
        Map<String, String> fields = message.getValue();
        String tripIdRaw = fields.get("tripId");
        String status = fields.get("status");
        log.info("[RESULT] received id={}, fields={}", message.getId(), fields);
        if (!StringUtils.hasText(tripIdRaw) || !StringUtils.hasText(status)) {
            log.warn("Invalid result message: {}", fields);
            ack(message);
            return;
        }
        Long tripId = Long.parseLong(tripIdRaw);

        if (ItineraryJobStatus.SUCCESS.name().equalsIgnoreCase(status)) {
            // 6) 성공 메시지면 payload를 파싱해 일정 반영 + 상태 갱신
            jobService.markProcessing(tripId);
            String payload = fields.getOrDefault("payload", "");
            if (StringUtils.hasText(payload)) {
                try {
                    log.info("트라이 진입");
                    AiItineraryResponse response = objectMapper.readValue(payload, AiItineraryResponse.class);
                    log.info("ai결과 가져옴: "+response);
                    processor.processResponse(response);
                    log.info("프로세서 메서드 실행완료");
                    updateTripStatus(tripId, TripStatus.DONE);
                    jobService.markSuccess(tripId);
                    log.info("여행상태 셋 & 잡 성공마킹 완료");
                } catch (Exception ex) {
                    log.error("일정 결과큐 프로세싱 중 예외 발생", ex);
                    updateTripStatus(tripId, TripStatus.CANCELED);
                    jobService.markFail(tripId, "RESULT_PROCESSING_FAILED");
                }
            } else {
                updateTripStatus(tripId, TripStatus.CANCELED);
                jobService.markFail(tripId, "EMPTY_RESULT_PAYLOAD");
            }
        } else if (ItineraryJobStatus.FAIL.name().equalsIgnoreCase(status)) {
            // 7) 실패 메시지면 실패 사유와 trip 상태를 반영
            String errorMessage = fields.get("errorMessage");
            updateTripStatus(tripId, TripStatus.CANCELED);
            jobService.markFail(tripId, errorMessage);
        } else {
            log.warn("Unknown status: {}, fields={}", status, fields);
        }
        // 8) 마지막에 XACK으로 "해당 group에서 처리 완료"를 확정한다.
        ack(message);
    }

    private void ack(MapRecord<String, String, String> message) {
        try {
            redisTemplate.opsForStream().acknowledge(streamProperties.getAiResultsKey(), jobProperties.getConsumerGroup(), message.getId());
            log.info("[RESULT] acked id={}", message.getId());
        } catch (Exception ex) {
            log.warn("Failed to ack result message: {}", message.getId(), ex);
        }
    }

    private void updateTripStatus(Long tripId, TripStatus status) {
        Trip trip = tripRepository.findById(tripId).orElse(null);
        if (trip == null) {
            return;
        }
        trip.updateStatus(status);
        tripRepository.save(trip);
    }
}
