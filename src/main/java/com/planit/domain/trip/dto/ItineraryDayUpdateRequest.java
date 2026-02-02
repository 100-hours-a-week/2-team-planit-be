package com.planit.domain.trip.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalTime;
import java.util.List;

public record ItineraryDayUpdateRequest(
        Long dayId,
        List<PlaceUpdate> places
) {

    public record PlaceUpdate(
            Long activityId,
            String placeName,
            @JsonFormat(pattern = "HH:mm")
            LocalTime startTime,
            Integer durationMinutes,
            Integer cost,
            String memo
    ) {
    }
}
