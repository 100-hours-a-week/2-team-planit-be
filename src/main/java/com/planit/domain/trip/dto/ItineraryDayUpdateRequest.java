package com.planit.domain.trip.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.List;

public record ItineraryDayUpdateRequest(
        @NotNull
        Long tripId,
        @NotNull
        Long dayId,
        List<PlaceUpdate> places
) {

    public record PlaceUpdate(
            Long activityId,
            String placeName,
            String placeId,
            String googleMapUrl,
            @JsonFormat(pattern = "HH:mm")
            LocalTime startTime,
            Integer durationMinutes,
            Integer cost,
            String memo
    ) {
    }
}
