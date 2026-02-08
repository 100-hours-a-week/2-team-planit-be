package com.planit.domain.place.client;

import com.planit.domain.place.exception.PlaceSearchException;
import com.planit.global.common.exception.ErrorCode;
import java.net.SocketTimeoutException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GooglePlacesClient {

    private static final Logger logger = LoggerFactory.getLogger(GooglePlacesClient.class);
    private static final String FIELD_MASK =
            "places.id,places.displayName,places.formattedAddress,places.location,places.googleMapsUri";

    private final RestClient restClient;
    private final String apiKey;

    public GooglePlacesClient(
            RestClient googlePlacesRestClient,
            @Value("${google.maps.api-key}") String apiKey
    ) {
        this.restClient = googlePlacesRestClient;
        this.apiKey = apiKey;
    }

    public GooglePlacesResponse searchText(GooglePlacesRequest request) {
        try {
            return restClient.post()
                    .uri("/v1/places:searchText?languageCode=ko")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", FIELD_MASK)
                    .body(request)
                    .retrieve()
                    .body(GooglePlacesResponse.class);
        } catch (RestClientResponseException ex) {
            HttpStatus status = HttpStatus.resolve(ex.getRawStatusCode());
            logger.warn("Google Places error: status={}, body={}", ex.getRawStatusCode(), ex.getResponseBodyAsString());
            throw new PlaceSearchException(
                    ErrorCode.PLACE_003,
                    status == null ? HttpStatus.BAD_GATEWAY : HttpStatus.BAD_GATEWAY,
                    "Google Places API error"
            );
        } catch (ResourceAccessException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof SocketTimeoutException) {
                logger.warn("Google Places timeout", ex);
                throw new PlaceSearchException(ErrorCode.PLACE_004, HttpStatus.GATEWAY_TIMEOUT, "Google Places timeout");
            }
            logger.warn("Google Places access error", ex);
            throw new PlaceSearchException(ErrorCode.PLACE_003, HttpStatus.BAD_GATEWAY, "Google Places access error");
        } catch (Exception ex) {
            logger.warn("Google Places unexpected error", ex);
            throw new PlaceSearchException(ErrorCode.PLACE_003, HttpStatus.BAD_GATEWAY, "Google Places error");
        }
    }
}
