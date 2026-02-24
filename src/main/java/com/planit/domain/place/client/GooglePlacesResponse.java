package com.planit.domain.place.client;

import java.util.List;

public record GooglePlacesResponse(

        List<Place> places
) {
    public record Place(
            String id,
            DisplayName displayName,
            String formattedAddress,
            Location location,
            String googleMapsUri
    ) {
    }

    public record DisplayName(String text) {
    }

    public record Location(double latitude, double longitude) {
    }
}
