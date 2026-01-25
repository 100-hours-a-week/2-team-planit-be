package com.planit.domain.trip.service;

import com.planit.domain.trip.dto.AiItineraryRequest;
import com.planit.domain.trip.dto.AiItineraryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AiItineraryClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public AiItineraryClient(RestTemplate restTemplate,
                             @Value("${ai.base-url:http://localhost:8000}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public AiItineraryResponse requestItinerary(AiItineraryRequest request) {
        // RestTemplate: Spring's basic HTTP client
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AiItineraryRequest> entity = new HttpEntity<>(request, headers);
        return restTemplate.postForObject(baseUrl + "/api/v1/itinerary", entity, AiItineraryResponse.class);
    }
}
