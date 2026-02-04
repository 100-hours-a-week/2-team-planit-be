package com.planit.domain.trip.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.List;

public record TripListResponse(
        List<TripSummary> trips
) {

    public record TripSummary(
            Long tripId,
            String title,
            @JsonFormat(pattern = "yyyy-MM-dd")
            LocalDate startDate,
            @JsonFormat(pattern = "yyyy-MM-dd")
            LocalDate endDate,
            String travelCity
    ) {
    }
}
