package com.planit.domain.trip.dto;

public record TripCreateResponse(
        Long tripId,
        String inviteCode
) {
}
