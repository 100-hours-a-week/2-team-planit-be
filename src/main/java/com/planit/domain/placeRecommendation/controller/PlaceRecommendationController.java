package com.planit.domain.placeRecommendation.controller;

import com.planit.domain.placeRecommendation.dto.PlaceRecommendationDetailResponse;
import com.planit.domain.placeRecommendation.dto.PlaceRecommendationSearchResponse;
import com.planit.domain.placeRecommendation.service.PlaceRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/place-recommendations", produces = MediaType.APPLICATION_JSON_VALUE)
public class PlaceRecommendationController {

    private final PlaceRecommendationService service;

    public PlaceRecommendationController(PlaceRecommendationService service) {
        this.service = service;
    }

    @Operation(summary = "Places API proxy: query-based 추천 검색")
    @GetMapping("/search")
    public List<PlaceRecommendationSearchResponse> search(
            @RequestParam String query,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country
    ) {
        return service.search(query, city, country);
    }

    @Operation(summary = "Places API proxy: 단일 장소 세부 정보")
    @GetMapping("/places/{placeId}")
    public PlaceRecommendationDetailResponse getPlace(@PathVariable String placeId) {
        return service.getPlaceDetail(placeId);
    }
}
