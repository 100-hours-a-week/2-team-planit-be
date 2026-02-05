package com.planit.domain.trip.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

public record ItineraryResponse(
        Long tripId,
        String title,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate endDate,
        Boolean isOwner,
        List<ItineraryDayResponse> itineraries
) {
}
