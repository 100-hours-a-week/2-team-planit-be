package com.planit.domain.trip.controller;

import com.planit.domain.trip.dto.GroupJoinResponse;
import com.planit.domain.trip.dto.GroupSubmitRequest;
import com.planit.domain.trip.dto.TripGroupStatusResponse;
import com.planit.domain.trip.service.TripGroupService;
import com.planit.global.common.exception.UnauthorizedAccessException;
import com.planit.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GroupController {

    private final TripGroupService tripGroupService;

    public GroupController(TripGroupService tripGroupService) {
        this.tripGroupService = tripGroupService;
    }

    @GetMapping("/groups/join/{inviteCode}")
    public ResponseEntity<ApiResponse<GroupJoinResponse>> getJoinInfo(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String inviteCode
    ) {
        // [같이가기 입력 전 단계] 초대코드 기준으로 현재 그룹 상태/내 제출값을 조회한다.
        if (principal == null) {
            throw new UnauthorizedAccessException();
        }
        GroupJoinResponse response = tripGroupService.getJoinInfo(inviteCode, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/groups/join/{inviteCode}/submit")
    public ResponseEntity<ApiResponse<TripGroupStatusResponse>> submitGroupInput(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String inviteCode,
            @Valid @RequestBody GroupSubmitRequest request
    ) {
        // [같이가기 입력 단계] 멤버가 travelTheme/wantedPlace를 제출하는 진입점
        if (principal == null) {
            throw new UnauthorizedAccessException();
        }
        TripGroupStatusResponse response = tripGroupService.submit(inviteCode, principal.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/trips/{tripId}/group")
    public ResponseEntity<ApiResponse<TripGroupStatusResponse>> getTripGroupStatus(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long tripId
    ) {
        if (principal == null) {
            throw new UnauthorizedAccessException();
        }
        TripGroupStatusResponse response = tripGroupService.getByTrip(tripId, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/trips/{tripId}/cancel-waiting")
    public ResponseEntity<ApiResponse<Void>> cancelWaiting(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long tripId
    ) {
        if (principal == null) {
            throw new UnauthorizedAccessException();
        }
        tripGroupService.cancelWaiting(tripId, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
