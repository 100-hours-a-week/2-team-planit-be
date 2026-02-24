package com.planit.domain.place.client;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GooglePlacesRequest(
        String textQuery,
        LocationRestriction locationRestriction
) {
    public record LocationRestriction(Rectangle rectangle) {
    }

    public record Rectangle(Point low, Point high) {
    }

    public record Point(double latitude, double longitude) {
    }
}
