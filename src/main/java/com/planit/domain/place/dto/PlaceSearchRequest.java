package com.planit.domain.place.dto;

import com.planit.domain.place.model.DestinationCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlaceSearchRequest(
        @NotNull
        DestinationCode destinationCode,
        @NotBlank
        @Size(max = 100)
        String query
) {
}
