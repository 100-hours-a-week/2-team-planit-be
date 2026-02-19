package com.planit.domain.trip.service;

import com.planit.domain.ai.config.AiProperties;
import com.planit.domain.trip.dto.AiItineraryRequest;
import com.planit.domain.trip.dto.AiItineraryResponse;
import com.planit.domain.trip.dto.AiItineraryActivityResponse;
import com.planit.domain.trip.dto.AiItineraryDayResponse;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * AI 여행 일정 생성 서버와 통신하는 클라이언트.
 * @RefreshScope: Actuator의 /actuator/refresh 호출 시 재시작 없이 설정 갱신 가능
 */
@RefreshScope
@Component
public class AiItineraryClient {

    private final RestTemplate restTemplate;
    private final AiProperties aiProperties;

    // 생성자로 client 클래스 필드 주입
    public AiItineraryClient(RestTemplate restTemplate,
                             AiProperties aiProperties) {
        this.restTemplate = restTemplate;
        this.aiProperties = aiProperties;
    }

    // AI 서버 요청 메서드
    public AiItineraryResponse requestItinerary(AiItineraryRequest request) {
        if (aiProperties.isMockEnabled()) {
            //throw new RestClientException("ai서버요청 에러응답 테스트용");
            return createDummyResponse(request);
        }
        // RestTemplate: 스프링 기본 HTTP 클라이언트
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AiItineraryRequest> entity = new HttpEntity<>(request, headers);
        return restTemplate.postForObject(aiProperties.getBaseUrl() + "/api/v1/itinerary", entity, AiItineraryResponse.class);
        // postForObject = throws RestClientException
    }



    private AiItineraryResponse createDummyResponse(AiItineraryRequest request) {
        LocalDate baseDate = request.arrivalDate() != null ? request.arrivalDate() : LocalDate.now();

        AiItineraryActivityResponse activity1 =
                new AiItineraryActivityResponse(
                        "면식당",
                        null,
                        "Restaurant",
                        1,
                        LocalTime.of(9, 0),
                        120,
                        50000,
                        "더미 메모",
                        "https://map.example/1"
                );
        AiItineraryActivityResponse activity2 =
                new AiItineraryActivityResponse(
                        null,
                        "bus",
                        "route",
                        2,
                        LocalTime.of(11, 0),
                        30,
                        null,
                        "더미 메모",
                        null
                );
        AiItineraryActivityResponse activity3 =
                new AiItineraryActivityResponse(
                        "사자 동굴",
                        null,
                        "Attraction",
                        3,
                        LocalTime.of(11, 30),
                        180,
                        30000,
                        "더미 메모",
                        "https://map.example/2"
                );
        AiItineraryDayResponse day1 = new AiItineraryDayResponse(1, baseDate, List.of(activity1, activity2, activity3));

        AiItineraryActivityResponse activity4 =
                new AiItineraryActivityResponse(
                        "초밥집",
                        null,
                        "Restaurant",
                        1,
                        LocalTime.of(10, 0),
                        90,
                        40000,
                        "더미 메모",
                        "https://map.example/3"
                );
        AiItineraryActivityResponse activity5 =
                new AiItineraryActivityResponse(
                        null,
                        "walk",
                        "route",
                        2,
                        LocalTime.of(11, 30),
                        45,
                        null,
                        "더미 메모",
                        null
                );
        AiItineraryActivityResponse activity6 =
                new AiItineraryActivityResponse(
                        "해변 산책",
                        null,
                        "Attraction",
                        3,
                        LocalTime.of(12, 15),
                        150,
                        60000,
                        "더미 메모",
                        "https://map.example/4"
                );
        AiItineraryDayResponse day2 =
                new AiItineraryDayResponse(2, baseDate.plusDays(1), List.of(activity4, activity5, activity6));

        return new AiItineraryResponse("SUCCESS", request.tripId(), List.of(day1, day2));
    }
}
