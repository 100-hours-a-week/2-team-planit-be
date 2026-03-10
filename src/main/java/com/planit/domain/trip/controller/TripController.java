package com.planit.domain.trip.controller;

import com.planit.domain.trip.dto.ItineraryDayUpdateRequest;
import com.planit.domain.trip.dto.TripCreateRequest;
import com.planit.domain.trip.dto.TripCreateResponse;
import com.planit.domain.trip.dto.TripCreateResult;
import com.planit.domain.trip.dto.TripListResponse;
import com.planit.domain.trip.service.redisAccessor.ItineraryJobService;
import com.planit.domain.trip.service.ItineraryQueryService;
import com.planit.domain.trip.service.ItineraryUpdateService;
import com.planit.domain.trip.service.TripService;
import com.planit.global.common.exception.ErrorCode;
import com.planit.global.common.exception.UnauthorizedAccessException;
import com.planit.global.common.response.ApiResponse;
import com.planit.global.common.response.ErrorResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TripController {

    private final TripService tripService;
    private final ItineraryQueryService itineraryQueryService;
    private final ItineraryUpdateService itineraryUpdateService;
    private final ItineraryJobService itineraryJobService;

    public TripController(
            TripService tripService,
            ItineraryQueryService itineraryQueryService,
            ItineraryUpdateService itineraryUpdateService,
            ItineraryJobService itineraryJobService
    ) {
        this.tripService = tripService;
        this.itineraryQueryService = itineraryQueryService;
        this.itineraryUpdateService = itineraryUpdateService;
        this.itineraryJobService = itineraryJobService;
    }

    @PostMapping("/trips")
    public ResponseEntity<ApiResponse<TripCreateResponse>> createTrip(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody TripCreateRequest request
    ) {
        if (principal == null) {
            throw new UnauthorizedAccessException();
        }
        TripCreateResult created = tripService.createTrip(request, principal.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(new TripCreateResponse(created.tripId(), created.inviteCode())));
    }

    @GetMapping("/trips")
    public ResponseEntity<ApiResponse<TripListResponse>> getTrips(
            @AuthenticationPrincipal UserDetails principal
    ) {
        // [같이가기 출력 단계] "내 계획 목록" 조회 진입점
        if (principal == null) {
            throw new UnauthorizedAccessException();
        }
        TripListResponse response = tripService.getUserTrips(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/trips/{tripId}/itineraries")
    public ResponseEntity<?> getItineraries(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long tripId
    ) {
        if (principal == null) {
            throw new UnauthorizedAccessException();
        }
        return itineraryQueryService.getTripItineraries(tripId, principal.getUsername())
                .<ResponseEntity<?>>map(response -> ResponseEntity.ok(ApiResponse.success(response)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ErrorResponse.from(ErrorCode.TRIP_001)));
    }

    @GetMapping("/trips/{tripId}/itinerary-job")
    public ResponseEntity<?> getItineraryJob(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long tripId
    ) {
        if (principal == null) {
            throw new UnauthorizedAccessException();
        }
        return itineraryJobService.getStatus(tripId, principal.getUsername())
                .<ResponseEntity<?>>map(response -> ResponseEntity.ok(ApiResponse.success(response)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ErrorResponse.from(ErrorCode.TRIP_003)));
    }

    @PatchMapping("/trips/itineraries/days")
    public ResponseEntity<ApiResponse<Void>> updateDayPlaces(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody ItineraryDayUpdateRequest request
    ) {
        if (principal == null) {
            throw new UnauthorizedAccessException();
        }
        itineraryUpdateService.updateDayPlaces(request, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/trips/{tripId}")
    public ResponseEntity<ApiResponse<Void>> deleteTrip(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long tripId
    ) {
        if (principal == null) {
            throw new UnauthorizedAccessException();
        }
        tripService.deleteTrip(principal.getUsername(), tripId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
