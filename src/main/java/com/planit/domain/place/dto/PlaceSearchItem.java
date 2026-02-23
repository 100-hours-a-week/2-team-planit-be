package com.planit.domain.place.dto;

public record PlaceSearchItem(

        String googlePlaceId,
        String googleMapUrl,
        String name,
        String address,
        Marker marker
) {
    public record Marker(double lat, double lng) {
    }
}
