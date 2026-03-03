package com.planit.domain.trip.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.planit.domain.trip.entity.TripStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record GroupJoinResponse(
        Long tripId,
        String title,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate arrivalDate,
        @JsonFormat(pattern = "HH:mm")
        LocalTime arrivalTime,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate departureDate,
        @JsonFormat(pattern = "HH:mm")
        LocalTime departureTime,
        String travelCity,
        Integer totalBudget,
        Integer headCount,
        Long submittedCount,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime expiresAt,
        TripStatus status,
        List<String> leaderTravelTheme,
        List<String> leaderWantedPlace,
        List<String> myTravelTheme,
        List<String> myWantedPlace,
        boolean submitted
) {
}
