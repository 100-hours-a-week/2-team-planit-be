package com.planit.domain.place.mapper;

import com.planit.domain.place.client.GooglePlacesResponse;
import com.planit.domain.place.dto.PlaceSearchItem;
import com.planit.domain.place.dto.PlaceSearchResponse;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PlaceSearchMapper {

    private PlaceSearchMapper() {
    }

    public static PlaceSearchResponse toResponse(GooglePlacesResponse response) {
        List<GooglePlacesResponse.Place> places =
                response == null || response.places() == null ? Collections.emptyList() : response.places();
        List<PlaceSearchItem> items = places.stream()
                .map(PlaceSearchMapper::toItem)
                .toList();
        return new PlaceSearchResponse(items);
    }

    private static PlaceSearchItem toItem(GooglePlacesResponse.Place place) {
        String name = Optional.ofNullable(place.displayName())
                .map(GooglePlacesResponse.DisplayName::text)
                .orElse(null);
        double lat = place.location() == null ? 0.0 : place.location().latitude();
        double lng = place.location() == null ? 0.0 : place.location().longitude();
        return new PlaceSearchItem(
                place.id(),
                place.googleMapsUri(),
                name,
                place.formattedAddress(),
                new PlaceSearchItem.Marker(lat, lng)
        );
    }
}
