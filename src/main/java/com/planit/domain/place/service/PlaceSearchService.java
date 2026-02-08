package com.planit.domain.place.service;

import com.planit.domain.place.client.GooglePlacesClient;
import com.planit.domain.place.client.GooglePlacesRequest;
import com.planit.domain.place.client.GooglePlacesResponse;
import com.planit.domain.place.dto.PlaceSearchRequest;
import com.planit.domain.place.dto.PlaceSearchResponse;
import com.planit.domain.place.exception.PlaceSearchException;
import com.planit.domain.place.mapper.PlaceSearchMapper;
import com.planit.domain.place.model.DestinationCode;
import com.planit.domain.place.model.GeoRectangle;
import com.planit.global.common.exception.ErrorCode;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PlaceSearchService {

    private static final Logger logger = LoggerFactory.getLogger(PlaceSearchService.class);

    private final GooglePlacesClient googlePlacesClient;

    public PlaceSearchService(GooglePlacesClient googlePlacesClient) {
        this.googlePlacesClient = googlePlacesClient;
    }

    public PlaceSearchResponse search(PlaceSearchRequest request) {
        if (request.destinationCode() == null) {
            throw new PlaceSearchException(ErrorCode.PLACE_001, HttpStatus.BAD_REQUEST, "destinationCode is required");
        }
        String trimmedQuery = normalizeQuery(request.query());
        if (!StringUtils.hasText(trimmedQuery) || trimmedQuery.length() > 100) {
            throw new PlaceSearchException(ErrorCode.PLACE_002, HttpStatus.BAD_REQUEST, "query is invalid");
        }

        DestinationCode destinationCode = request.destinationCode();
        GeoRectangle rectangle = destinationCode.getRectangle();

        logger.info("Place search request: destinationCode={}, query={}", destinationCode.name(), trimmedQuery);

        GooglePlacesRequest placesRequest = new GooglePlacesRequest(
                trimmedQuery,
                new GooglePlacesRequest.LocationRestriction(
                        new GooglePlacesRequest.Rectangle(
                                new GooglePlacesRequest.Point(rectangle.low().latitude(), rectangle.low().longitude()),
                                new GooglePlacesRequest.Point(rectangle.high().latitude(), rectangle.high().longitude())
                        )
                )
        );

        GooglePlacesResponse response = googlePlacesClient.searchText(placesRequest);
        return PlaceSearchMapper.toResponse(response);
    }

    private String normalizeQuery(String query) {
        return query == null ? "" : query.trim();
    }
}
