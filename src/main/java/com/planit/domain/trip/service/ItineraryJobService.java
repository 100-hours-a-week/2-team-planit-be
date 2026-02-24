package com.planit.domain.trip.service;

import com.planit.domain.trip.config.ItineraryJobProperties;
import com.planit.domain.trip.dto.ItineraryJobResponse;
import com.planit.domain.trip.entity.Trip;
import com.planit.domain.trip.repository.TripRepository;
import com.planit.domain.user.entity.User;
import com.planit.domain.user.repository.UserRepository;
import com.planit.global.common.exception.BusinessException;
import com.planit.global.common.exception.ErrorCode;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItineraryJobService {
    private static final Logger log = LoggerFactory.getLogger(ItineraryJobService.class);
    private final ItineraryJobRepository jobRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final ItineraryJobProperties jobProperties;

    public void initPending(Long tripId) {
        log.info("[JOB] init PENDING tripId={}", tripId);
        jobRepository.initPending(tripId);
        jobRepository.expire(tripId, jobProperties.getJobTtlSeconds());
    }

    public void markProcessing(Long tripId) {
        log.info("[JOB] mark PROCESSING tripId={}", tripId);
        jobRepository.updateStatus(tripId, ItineraryJobStatus.PROCESSING, null);
        jobRepository.expire(tripId, jobProperties.getJobTtlSeconds());
    }

    public void markSuccess(Long tripId) {
        log.info("[JOB] mark SUCCESS tripId={}", tripId);
        jobRepository.updateStatus(tripId, ItineraryJobStatus.SUCCESS, null);
        jobRepository.expire(tripId, jobProperties.getJobTtlSeconds());
    }

    public void markFail(Long tripId, String errorMessage) {
        log.warn("[JOB] mark FAIL tripId={}, error={}", tripId, errorMessage);
        jobRepository.updateStatus(tripId, ItineraryJobStatus.FAIL, errorMessage);
        jobRepository.expire(tripId, jobProperties.getJobTtlSeconds());
    }

    public Optional<ItineraryJobResponse> getStatus(Long tripId, String loginId) {
        log.debug("[JOB] get status tripId={}, loginId={}", tripId, loginId);
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_001));
        User user = userRepository.findByLoginIdAndDeletedFalse(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));
        if (!trip.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.TRIP_006);
        }

        Optional<Map<String, String>> statusOpt = jobRepository.findStatus(tripId);
        if (statusOpt.isEmpty()) {
            log.warn("[JOB] status not found tripId={}", tripId);
            return Optional.empty();
        }
        Map<String, String> status = statusOpt.get();
        log.debug("[JOB] status found tripId={}, status={}", tripId, status.get("status"));
        return Optional.of(new ItineraryJobResponse(
                tripId,
                status.getOrDefault("status", ItineraryJobStatus.PENDING.name()),
                emptyToNull(status.get("errorMessage")),
                status.get("updatedAt")
        ));
    }

    private String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
