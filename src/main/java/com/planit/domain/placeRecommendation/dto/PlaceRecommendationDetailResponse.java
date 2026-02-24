package com.planit.domain.placeRecommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PlaceRecommendationDetailResponse {

    private final String placeId;
    private final String name;
    private final String city;
    private final String country;
    private final Double latitude;
    private final Double longitude;
    private final String photoUrl;
    private final String googleMapsUrl;
}
