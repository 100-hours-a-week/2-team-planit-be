package com.planit.domain.trip.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record GroupSubmitRequest(
        @NotEmpty
        List<String> travelTheme,
        List<String> wantedPlace
) {
}
