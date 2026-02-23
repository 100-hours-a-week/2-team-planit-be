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
    private static final String PHOTO_BASE_URL = "https://maps.googleapis.com/maps/api/place/photo";

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
        GooglePlacesRequest placesRequest = new GooglePlacesRequest(composedQuery, WORLD_RESTRICTION);
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
        if (response == null || response.status() == null) {
            throw new PlaceSearchException(ErrorCode.PLACE_003, HttpStatus.NOT_FOUND, "place not found");
        }
        switch (response.status()) {
            case "OK" -> {
                PlaceDetailsResponse.PlaceResult result = response.result();
                if (result == null) {
                    throw new PlaceSearchException(ErrorCode.PLACE_003, HttpStatus.NOT_FOUND, "place not found");
                }
                String city = findComponent(result.address_components(), "locality");
                if (city == null) {
                    city = findComponent(result.address_components(), "administrative_area_level_2");
                }
                String country = findComponent(result.address_components(), "country");
                String photoUrl = buildPhotoUrl(result.photos());
                Double latitude = result.geometry() != null && result.geometry().location() != null
                        ? result.geometry().location().lat()
                        : null;
                Double longitude = result.geometry() != null && result.geometry().location() != null
                        ? result.geometry().location().lng()
                        : null;
                return new PlaceRecommendationDetailResponse(
                        result.place_id(),
                        result.name(),
                        city,
                        country,
                        latitude,
                        longitude,
                        photoUrl,
                        MAPS_PLACE_URL_PREFIX + result.place_id()
                );
            }
            case "ZERO_RESULTS", "NOT_FOUND" -> throw new PlaceSearchException(ErrorCode.PLACE_003, HttpStatus.NOT_FOUND, "place not found");
            case "INVALID_REQUEST" -> throw new PlaceSearchException(ErrorCode.PLACE_002, HttpStatus.BAD_REQUEST, "invalid placeId");
            default -> throw new PlaceSearchException(ErrorCode.PLACE_003, HttpStatus.BAD_GATEWAY, "Google Places details error");
        }
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
                .map(PlaceDetailsResponse.AddressComponent::long_name)
                .findFirst()
                .orElse(null);
    }

    private String buildPhotoUrl(List<PlaceDetailsResponse.Photo> photos) {
        if (photos == null || photos.isEmpty()) {
            return null;
        }
        String reference = photos.get(0).photo_reference();
        if (!StringUtils.hasText(reference)) {
            return null;
        }
        return PHOTO_BASE_URL + "?maxwidth=400&photoreference=" + reference + "&key=" + apiKey;
    }
}
