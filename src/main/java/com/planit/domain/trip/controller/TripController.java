package com.planit.domain.trip.controller;

//import com.planit.domain.trip.dto.ItineraryRegenerateRequest;
import com.planit.domain.trip.dto.ItineraryDayUpdateRequest;
import com.planit.domain.trip.dto.ItineraryResponse;
import com.planit.domain.trip.dto.TripCreateRequest;
import com.planit.domain.trip.dto.TripCreateResponse;
import com.planit.domain.trip.dto.TripListResponse;
import com.planit.domain.trip.service.ItineraryQueryService;
import com.planit.domain.trip.service.ItineraryUpdateService;
import com.planit.domain.trip.service.TripService;
import com.planit.global.common.exception.ErrorCode;
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

    public TripController(
            TripService tripService,
            ItineraryQueryService itineraryQueryService,
            ItineraryUpdateService itineraryUpdateService
    ) {
        this.tripService = tripService;
        this.itineraryQueryService = itineraryQueryService;
        this.itineraryUpdateService = itineraryUpdateService;
    }

    @PostMapping("/trips")
    public ResponseEntity<ApiResponse<TripCreateResponse>> createTrip(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody TripCreateRequest request
    ) {
        // 인증된 유저 기준으로 여행 생성 및 AI 큐 적재
        Long tripId = tripService.createTrip(request, principal.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(new TripCreateResponse(tripId)));
    }

    @GetMapping("/trips")
    public ResponseEntity<ApiResponse<TripListResponse>> getTrips(
            @AuthenticationPrincipal UserDetails principal
    ) {
        TripListResponse response = tripService.getUserTrips(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/trips/{tripId}/itineraries")
    public ResponseEntity<?> getItineraries(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long tripId
    ) {
        return itineraryQueryService.getTripItineraries(tripId, principal.getUsername())
                .<ResponseEntity<?>>map(response -> ResponseEntity.ok(ApiResponse.success(response)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ErrorResponse.from(ErrorCode.TRIP_001)));
    }

    @PatchMapping("/trips/itineraries/days")
    public ResponseEntity<ApiResponse<Void>> updateDayPlaces(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody ItineraryDayUpdateRequest request
    ) {
        // 특정 일자의 장소 정보만 부분 수정
        itineraryUpdateService.updateDayPlaces(request, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/trips/{tripId}")
    public ResponseEntity<ApiResponse<Void>> deleteTrip(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long tripId
    ) {
        // 유저 소유의 여행 삭제
        tripService.deleteTrip(principal.getUsername(), tripId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

}
