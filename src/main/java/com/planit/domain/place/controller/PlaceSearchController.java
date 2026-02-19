package com.planit.domain.place.controller;

import com.planit.domain.place.dto.PlaceSearchRequest;
import com.planit.domain.place.dto.PlaceSearchResponse;
import com.planit.domain.place.service.PlaceSearchService;
import com.planit.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/places")
public class PlaceSearchController {


    private final PlaceSearchService placeSearchService;

    public PlaceSearchController(PlaceSearchService placeSearchService) {
        this.placeSearchService = placeSearchService;
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<PlaceSearchResponse>> search(@Valid @RequestBody PlaceSearchRequest request) {
        PlaceSearchResponse response = placeSearchService.search(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
