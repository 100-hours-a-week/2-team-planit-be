package com.planit.domain.placeRecommendation.client;

import com.planit.domain.place.exception.PlaceSearchException;
import com.planit.global.common.exception.ErrorCode;
import java.net.SocketTimeoutException;
import java.util.List;
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
public class GooglePlaceDetailsClient {

    private static final Logger logger = LoggerFactory.getLogger(GooglePlaceDetailsClient.class);
    private static final String FIELD_MASK =
            "place_id,name,formatted_address,address_components,photos,geometry/location";

    private final RestClient restClient;
    private final String apiKey;

    public GooglePlaceDetailsClient(
            RestClient googleMapsRestClient,
            @Value("${google.maps.api-key}") String apiKey
    ) {
        this.restClient = googleMapsRestClient;
        this.apiKey = apiKey;
    }

    public PlaceDetailsResponse getPlaceDetails(String placeId) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/maps/api/place/details/json")
                            .queryParam("place_id", placeId)
                            .queryParam("fields", FIELD_MASK)
                            .queryParam("key", apiKey)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(PlaceDetailsResponse.class);
        } catch (RestClientResponseException ex) {
            HttpStatus status = HttpStatus.resolve(ex.getRawStatusCode());
            logger.warn("Google Places details error: status={}, body={}", ex.getRawStatusCode(), ex.getResponseBodyAsString());
            throw new PlaceSearchException(
                    ErrorCode.PLACE_003,
                    status == null ? HttpStatus.BAD_GATEWAY : HttpStatus.BAD_GATEWAY,
                    "Google Places details error"
            );
        } catch (ResourceAccessException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof SocketTimeoutException) {
                logger.warn("Google Places details timeout", ex);
                throw new PlaceSearchException(ErrorCode.PLACE_004, HttpStatus.GATEWAY_TIMEOUT, "Google Places details timeout");
            }
            logger.warn("Google Places details access error", ex);
            throw new PlaceSearchException(ErrorCode.PLACE_003, HttpStatus.BAD_GATEWAY, "Google Places details access error");
        } catch (Exception ex) {
            logger.warn("Google Places details unexpected error", ex);
            throw new PlaceSearchException(ErrorCode.PLACE_003, HttpStatus.BAD_GATEWAY, "Google Places details error");
        }
    }

    public record PlaceDetailsResponse(String status, PlaceResult result, String error_message) {
        public record PlaceResult(
                String place_id,
                String name,
                String formatted_address,
                List<AddressComponent> address_components,
                List<Photo> photos,
                Geometry geometry
        ) {
        }

        public record Geometry(Location location) {
        }

        public record Location(double lat, double lng) {
        }

        public record AddressComponent(String long_name, List<String> types) {
        }

        public record Photo(String photo_reference) {
        }
    }
}
