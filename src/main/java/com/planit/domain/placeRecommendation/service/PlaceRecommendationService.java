package com.planit.domain.placeRecommendation.service;

import com.planit.domain.place.client.GooglePlacesClient;
import com.planit.domain.place.client.GooglePlacesRequest;
import com.planit.domain.place.client.GooglePlacesResponse;
import com.planit.domain.place.exception.PlaceSearchException;
import com.planit.domain.placeRecommendation.client.GooglePlaceDetailsClient;
import com.planit.domain.placeRecommendation.client.GooglePlaceDetailsClient.PlaceDetailsResponse;
import com.planit.domain.placeRecommendation.dto.PlaceRecommendationDetailResponse;
import com.planit.domain.placeRecommendation.dto.PlaceRecommendationSearchResponse;
import com.planit.global.common.exception.ErrorCode;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PlaceRecommendationService {

    private static final GooglePlacesRequest.LocationRestriction WORLD_RESTRICTION =
            new GooglePlacesRequest.LocationRestriction(
                    new GooglePlacesRequest.Rectangle(
                            new GooglePlacesRequest.Point(-90d, -180d),
                            new GooglePlacesRequest.Point(90d, 180d)
                    )
            );
    private static final int MAX_SEARCH_RESULTS = 10;
    private static final String MAPS_PLACE_URL_PREFIX = "https://www.google.com/maps/place/?q=place_id:";

    private final GooglePlacesClient googlePlacesClient;
    private final GooglePlaceDetailsClient googlePlaceDetailsClient;
    private final String apiKey;

    public PlaceRecommendationService(
            GooglePlacesClient googlePlacesClient,
            GooglePlaceDetailsClient googlePlaceDetailsClient,
            @Value("${google.maps.api-key}") String apiKey
    ) {
        this.googlePlacesClient = googlePlacesClient;
        this.googlePlaceDetailsClient = googlePlaceDetailsClient;
        this.apiKey = apiKey;
    }

    public List<PlaceRecommendationSearchResponse> search(String query, String city, String country) {
        String normalized = normalizeQuery(query);
        if (!StringUtils.hasText(normalized)) {
            throw new PlaceSearchException(ErrorCode.PLACE_002, HttpStatus.BAD_REQUEST, "query is required");
        }
        String composedQuery = Stream.of(normalized, city, country)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" "));
        if (!StringUtils.hasText(composedQuery)) {
            throw new PlaceSearchException(ErrorCode.PLACE_002, HttpStatus.BAD_REQUEST, "query is required");
        }
        GooglePlacesRequest placesRequest = new GooglePlacesRequest(composedQuery, null);
        GooglePlacesResponse response = googlePlacesClient.searchText(placesRequest);
        if (response == null || response.places() == null) {
            return Collections.emptyList();
        }
        return response.places()
                .stream()
                .limit(MAX_SEARCH_RESULTS)
                .map(place -> {
                    String primaryText = place.displayName() != null && place.displayName().text() != null
                            ? place.displayName().text()
                            : place.formattedAddress();
                    if (primaryText == null) {
                        primaryText = "";
                    }
                    String secondaryText = place.formattedAddress();
                    if (secondaryText == null) {
                        secondaryText = "";
                    }
                    return new PlaceRecommendationSearchResponse(
                            place.id(),
                            primaryText,
                            secondaryText
                    );
                })
                .collect(Collectors.toList());
    }

    public PlaceRecommendationDetailResponse getPlaceDetail(String placeId) {
        if (!StringUtils.hasText(placeId)) {
            throw new PlaceSearchException(ErrorCode.PLACE_002, HttpStatus.BAD_REQUEST, "placeId is required");
        }
        PlaceDetailsResponse response = googlePlaceDetailsClient.getPlaceDetails(placeId);
        if (response == null || !StringUtils.hasText(response.id())) {
            throw new PlaceSearchException(ErrorCode.PLACE_003, HttpStatus.NOT_FOUND, "place not found");
        }
        String googlePlaceId = extractGooglePlaceId(response.id());
        if (!StringUtils.hasText(googlePlaceId)) {
            throw new PlaceSearchException(ErrorCode.PLACE_003, HttpStatus.NOT_FOUND, "place not found");
        }
        String city = findComponent(response.addressComponents(), "locality");
        if (city == null) {
            city = findComponent(response.addressComponents(), "administrative_area_level_2");
        }
        String country = findComponent(response.addressComponents(), "country");
        String placeName = response.displayName() != null && StringUtils.hasText(response.displayName().text())
                ? response.displayName().text()
                : response.formattedAddress();
        Double latitude = response.location() != null ? response.location().latitude() : null;
        Double longitude = response.location() != null ? response.location().longitude() : null;
        String photoUrl = null;
        if (response.photos() != null && !response.photos().isEmpty()) {
            photoUrl = googlePlaceDetailsClient.fetchPhotoMediaUrl(response.photos().get(0).name());
        }
        return new PlaceRecommendationDetailResponse(
                googlePlaceId,
                placeName,
                city,
                country,
                latitude,
                longitude,
                photoUrl,
                MAPS_PLACE_URL_PREFIX + googlePlaceId
        );
    }

    private String normalizeQuery(String query) {
        return query == null ? "" : query.trim();
    }

    private String findComponent(List<PlaceDetailsResponse.AddressComponent> components, String type) {
        if (components == null) {
            return null;
        }
        return components.stream()
                .filter(component -> component.types().contains(type))
                .map(PlaceDetailsResponse.AddressComponent::longText)
                .findFirst()
                .orElse(null);
    }

    private String extractGooglePlaceId(String id) {
        if (!StringUtils.hasText(id)) {
            return null;
        }
        int lastSlash = id.lastIndexOf('/');
        return lastSlash >= 0 && lastSlash < id.length() - 1 ? id.substring(lastSlash + 1) : id;
    }
}
