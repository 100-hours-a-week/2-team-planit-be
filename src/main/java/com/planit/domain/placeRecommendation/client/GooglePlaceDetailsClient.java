package com.planit.domain.placeRecommendation.client;

import com.planit.domain.place.exception.PlaceSearchException;
import com.planit.global.common.exception.ErrorCode;
import java.net.SocketTimeoutException;
import java.util.List;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GooglePlaceDetailsClient {

    private static final Logger logger = LoggerFactory.getLogger(GooglePlaceDetailsClient.class);
    private static final String FIELD_MASK =
            "id,displayName,formattedAddress,addressComponents,photos,location";
    private static final HttpClient PHOTO_HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

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
                            .path("/v1/places/{placeId}")
                            .queryParam("languageCode", "ko")
                            .build(placeId))
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", FIELD_MASK)
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

    public String fetchPhotoMediaUrl(String photoName) {
        if (!StringUtils.hasText(photoName)) {
            return null;
        }
        try {
            String path = "/v1/" + photoName + "/media";
            URI uri = new URI("https", "places.googleapis.com", path, "maxWidthPx=400", null);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .GET()
                    .header("X-Goog-Api-Key", apiKey)
                    .build();
            HttpResponse<Void> response = PHOTO_HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() == 301 || response.statusCode() == 302) {
                return response.headers().firstValue("Location").orElse(null);
            }
            if (response.statusCode() == 200) {
                return uri.toString();
            }
        } catch (Exception ex) {
            logger.warn("Google Places photo fetch error for {}: {}", photoName, ex.getMessage());
        }
        return null;
    }

    public record PlaceDetailsResponse(
            String id,
            DisplayName displayName,
            String formattedAddress,
            Location location,
            List<AddressComponent> addressComponents,
            List<Photo> photos
    ) {
        public record DisplayName(String text) {
        }

        public record Location(double latitude, double longitude) {
        }

        public record AddressComponent(String longText, List<String> types) {
        }

        public record Photo(String name) {
        }
    }
}
