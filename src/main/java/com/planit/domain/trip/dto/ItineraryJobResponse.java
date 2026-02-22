package com.planit.domain.trip.dto;

public record ItineraryJobResponse(
        Long tripId,
        String status,
        String errorMessage,
        String updatedAt
) {
}
