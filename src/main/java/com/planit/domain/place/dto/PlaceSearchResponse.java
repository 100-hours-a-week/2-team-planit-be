package com.planit.domain.place.dto;

import java.util.List;

public record PlaceSearchResponse(

        List<PlaceSearchItem> items
) {
}
