package com.planit.domain.trip.dto;

public record TripCreateResult(
        Long tripId,
        String inviteCode
) {
}
