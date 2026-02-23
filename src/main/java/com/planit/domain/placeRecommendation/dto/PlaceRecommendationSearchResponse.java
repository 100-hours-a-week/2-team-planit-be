package com.planit.domain.placeRecommendation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
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
}
