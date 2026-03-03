package com.planit.domain.placeRecommendation.dto;

import lombok.Getter;

@Getter
public class PlaceRecommendationDetailResponse {

    private final String placeId;
    private final String name;
    private final String city;
    private final String country;
    private final Double latitude;
    private final Double longitude;
    private final String photoUrl;
    private final String googleMapsUrl;

    public PlaceRecommendationDetailResponse(
            String placeId,
            String name,
            String city,
            String country,
            Double latitude,
            Double longitude,
            String photoUrl,
            String googleMapsUrl
    ) {
        this.placeId = placeId;
        this.name = name;
        this.city = city;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
        this.photoUrl = photoUrl;
        this.googleMapsUrl = googleMapsUrl;
    }

    public String getPlaceId() {
        return placeId;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }
}
