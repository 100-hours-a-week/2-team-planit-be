package com.planit.domain.trip.dto;

import java.util.List;

// DTO for AI response body
public record AiItineraryResponse(
        String message,
        List<ItineraryDto> itineraries
) {
}
