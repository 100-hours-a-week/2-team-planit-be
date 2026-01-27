package com.planit.domain.trip.service;

import com.planit.domain.trip.dto.AiItineraryRequest;
import com.planit.domain.trip.dto.TripCreateRequest;
import com.planit.domain.trip.entity.Trip;
import com.planit.domain.trip.entity.TripTheme;
import com.planit.domain.trip.repository.TripRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final AiItineraryQueue aiItineraryQueue;
    private final AiItineraryProcessor aiItineraryProcessor;
    private final boolean aiMockEnabled;

    public TripService(TripRepository tripRepository,
                       AiItineraryQueue aiItineraryQueue,
                       AiItineraryProcessor aiItineraryProcessor,
                       @Value("${ai.mock-enabled:false}") boolean aiMockEnabled) {
        this.tripRepository = tripRepository;
        this.aiItineraryQueue = aiItineraryQueue;
        this.aiItineraryProcessor = aiItineraryProcessor;
        this.aiMockEnabled = aiMockEnabled;
    }


    @Transactional
    public Long createTrip(TripCreateRequest request) {


        Trip trip = tripRepository.save(new Trip(
                request.title(),
                request.arrivalDate(),
                request.departureDate(),
                request.arrivalTime(),
                request.departureTime(),
                request.travelCity(),
                request.totalBudget()
        ));
        System.out.println("여행 저장 후 tripId 반환: " + trip.getId());



        // 테마 저장 (Trip 1 : Theme N)
        for (String theme : request.travelTheme()) {
            trip.addTheme(new TripTheme(trip, theme));
        }
        // System.out.println("테마 목록 할당 후 여행 재저장함");
        tripRepository.save(trip);

        // AI 서버 미사용 테스트를 위해 mock 모드에서는 즉시 처리
        AiItineraryJob job = new AiItineraryJob(new AiItineraryRequest(
                trip.getId(),
                trip.getArrivalDate(),
                trip.getArrivalTime(),
                trip.getDepartureDate(),
                trip.getDepartureTime(),
                trip.getTravelCity(),
                trip.getTotalBudget(),
                request.travelTheme()
        ));
        if (aiMockEnabled) {
            System.out.println("AI mock 모드: 즉시 일정 생성 처리");
            aiItineraryProcessor.process(job);
        } else {
            // AI 요청을 큐에 적재 후 즉시 응답
            aiItineraryQueue.enqueue(job);
        }

        return trip.getId();

    }


    















}
