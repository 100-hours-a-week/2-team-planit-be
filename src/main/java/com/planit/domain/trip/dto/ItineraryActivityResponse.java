package com.planit.domain.trip.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public record ItineraryActivityResponse(
        String type,
        Integer order,
        LocalTime startTime,
        LocalTime durationTime,
        BigDecimal cost,
        String transport,
        String placeId,
        String placeName,
        String googleMapUrl,
        String positionLat,
        String positionLng
) {
}
