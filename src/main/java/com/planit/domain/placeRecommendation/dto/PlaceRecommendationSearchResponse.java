package com.planit.domain.placeRecommendation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class PlaceRecommendationSearchResponse {

    private final String placeId;

    @JsonProperty("primaryText")
    private final String primaryText;

    @JsonProperty("secondaryText")
    private final String secondaryText;

    @JsonProperty("name")
    public String aliasName() {
        return primaryText;
    }

    @JsonProperty("addressText")
    public String aliasAddress() {
        return secondaryText;
    }

    public PlaceRecommendationSearchResponse(String placeId, String primaryText, String secondaryText) {
        this.placeId = placeId;
        this.primaryText = primaryText;
        this.secondaryText = secondaryText;
    }

    public String getPlaceId() {
        return placeId;
    }

    public String getPrimaryText() {
        return primaryText;
    }

    public String getSecondaryText() {
        return secondaryText;
    }
}
