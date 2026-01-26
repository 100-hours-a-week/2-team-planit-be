package com.planit.domain.trip.service;

import com.planit.domain.trip.dto.AiItineraryRequest;
import com.planit.domain.trip.dto.TripCreateRequest;
import com.planit.domain.trip.entity.Trip;
import com.planit.domain.trip.entity.TripTheme;
import com.planit.domain.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final AiItineraryQueue aiItineraryQueue;

    public TripService(TripRepository tripRepository, AiItineraryQueue aiItineraryQueue) {
        this.tripRepository = tripRepository;
        this.aiItineraryQueue = aiItineraryQueue;
    }


    @Transactional
    public Long createTrip(TripCreateRequest request) {


        /*
        Trip trip = tripRepository.save(new Trip(
                request.title(),
                request.arrivalDate(),
                request.departureDate(),
                request.arrivalTime(),
                request.departureTime(),
                request.travelCity(),
                request.totalBudget()
        ));
         */

        Trip trip = new Trip(
                request.title(),
                request.arrivalDate(),
                request.departureDate(),
                request.arrivalTime(),
                request.departureTime(),
                request.travelCity(),
                request.totalBudget()
        );
        System.out.println("여행 저장 후 trip객체 반환받음");


        // Themes: Trip 1 : Theme N
        for (String theme : request.travelTheme()) {
            trip.addTheme(new TripTheme(trip, theme));
        }
        System.out.println("테마 목록 할당 후 여행 재저장함");
        //tripRepository.save(trip);


        // Enqueue AI request and return immediately
        aiItineraryQueue.enqueue(new AiItineraryJob(new AiItineraryRequest(
                trip.getId(),
                trip.getArrivalDate(),
                trip.getArrivalTime(),
                trip.getDepartureDate(),
                trip.getDepartureTime(),
                trip.getTravelCity(),
                trip.getTotalBudget(),
                request.travelTheme()
        )));

        return trip.getId();

    
    /* 코덱스 - DB제외 버전

    @Transactional
    public Long createTrip(TripCreateRequest request) {
        // DB 저장을 생략하고 임시 tripId만 생성
        Long tripId = System.currentTimeMillis();
        System.out.println("[DB 생략] Trip 저장 예정 title=" + request.title()
                + ", travelCity=" + request.travelCity());
        for (String theme : request.travelTheme()) {
            System.out.println("[DB 생략] TripTheme 저장 예정 theme=" + theme);
        }

        // AI 요청은 정상 플로우로 큐에 적재
        aiItineraryQueue.enqueue(new AiItineraryJob(new AiItineraryRequest(
                tripId,
                request.arrivalDate(),
                request.arrivalTime(),
                request.departureDate(),
                request.departureTime(),
                request.travelCity(),
                request.totalBudget(),
                request.travelTheme()
        )));

        return tripId;

     */
    }
}
