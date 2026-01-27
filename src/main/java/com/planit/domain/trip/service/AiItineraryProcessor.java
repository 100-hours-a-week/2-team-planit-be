package com.planit.domain.trip.service;

import com.planit.domain.trip.dto.ActivityDto;
import com.planit.domain.trip.dto.AiItineraryResponse;
import com.planit.domain.trip.dto.ItineraryDto;
import com.planit.domain.trip.entity.ItineraryItem;
import com.planit.domain.trip.entity.ItineraryItemPlace;
import com.planit.domain.trip.entity.ItineraryItemTransport;
import com.planit.domain.trip.entity.Trip;
import com.planit.domain.trip.repository.ItineraryItemPlaceRepository;
import com.planit.domain.trip.repository.ItineraryItemRepository;
import com.planit.domain.trip.repository.ItineraryItemTransportRepository;
import com.planit.domain.trip.repository.TripRepository;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiItineraryProcessor {

    private final AiItineraryClient client;
    private final TripRepository tripRepository;
    private final ItineraryItemRepository itineraryItemRepository;
    private final ItineraryItemPlaceRepository placeRepository;
    private final ItineraryItemTransportRepository transportRepository;

    public AiItineraryProcessor(
            AiItineraryClient client,
            TripRepository tripRepository,
            ItineraryItemRepository itineraryItemRepository,
            ItineraryItemPlaceRepository placeRepository,
            ItineraryItemTransportRepository transportRepository
    ) {
        this.client = client;
        this.tripRepository = tripRepository;
        this.itineraryItemRepository = itineraryItemRepository;
        this.placeRepository = placeRepository;
        this.transportRepository = transportRepository;
    }

    @Transactional
    public void process(AiItineraryJob job) {
        // AI 서버로 일정 생성 요청
        AiItineraryResponse response = client.requestItinerary(job.request());
        if (response == null || response.itineraries() == null) {
            return;
        }

        // 여행이 존재할 때만 일정 저장
        Trip trip = tripRepository.findById(job.request().tripId()).orElse(null);
        if (trip == null) {
            return;
        }

        for (ItineraryDto itinerary : response.itineraries()) {

            // 일자별 일정 저장
            ItineraryItem item = itineraryItemRepository.save(new ItineraryItem(trip, itinerary.day()));

            List<ActivityDto> activities = itinerary.activities();
            if (activities == null) {
                continue;
            }
            int order = 1;
            for (ActivityDto activity : activities) {
                if (activity == null || activity.type() == null) {
                    order++;
                    continue;
                }
                // Route는 이동 이벤트, 나머지는 장소 이벤트로 처리
                if (isRoute(activity.type())) {
                    transportRepository.save(new ItineraryItemTransport(
                        item,
                        "UNKNOWN", // 이동수단 정보가 없어서 임시값 사용
                        order,
                        resolveStartTime(activity.startTime()),
                        resolveDuration(activity.duration())
                    ));
                } else {
                    placeRepository.save(new ItineraryItemPlace(
                        item,
                        activity.placeId(),
                        order,
                        resolveStartTime(activity.startTime()),
                        resolveDuration(activity.duration()),
                        resolveCost(activity.cost()),
                        null,
                        null,
                        null,
                        null
                    ));
                }
                order++;
            }
        }
    }

    private boolean isRoute(String type) {
        return "route".equalsIgnoreCase(type);
    }

    private LocalTime resolveStartTime(LocalTime startTime) {
        // 시작 시간이 없으면 00:00으로 보정
        return startTime != null ? startTime : LocalTime.MIDNIGHT;
    }

    private LocalTime resolveDuration(Integer durationMinutes) {
        if (durationMinutes == null || durationMinutes <= 0) {
            return LocalTime.MIDNIGHT;
        }
        // duration(분) -> LocalTime 변환
        return LocalTime.ofSecondOfDay(durationMinutes * 60L);
    }

    private BigDecimal resolveCost(Integer cost) {
        if (cost == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(cost);
    }
}
