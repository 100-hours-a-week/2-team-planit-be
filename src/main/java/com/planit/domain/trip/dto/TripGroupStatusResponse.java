package com.planit.domain.trip.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.planit.domain.trip.entity.TripStatus;
import java.time.LocalDateTime;

public record TripGroupStatusResponse(
        Long tripId,
        String inviteCode,
        Integer headCount,
        Long submittedCount,
        TripStatus status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime expiresAt
) {
}
