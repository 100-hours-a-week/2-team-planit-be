package com.planit.domain.trip.dto;

import java.time.LocalDate;
import java.util.List;

public record ItineraryDayResponse(
        int day,
        LocalDate date,
        List<ItineraryActivityResponse> activities
) {
}
