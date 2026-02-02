package com.planit.domain.trip.service;

import com.planit.domain.trip.dto.AiItineraryRequest;
import com.planit.domain.trip.dto.TripCreateRequest;
import com.planit.domain.trip.entity.ItineraryDay;
import com.planit.domain.trip.entity.Trip;
import com.planit.domain.trip.entity.TripTheme;
import com.planit.domain.trip.entity.WantedPlace;
import com.planit.domain.trip.repository.ItineraryDayRepository;
import com.planit.domain.trip.repository.ItineraryItemPlaceRepository;
import com.planit.domain.trip.repository.ItineraryItemTransportRepository;
import com.planit.domain.trip.repository.TripRepository;
import com.planit.domain.trip.repository.TripThemeRepository;
import com.planit.domain.trip.repository.WantedPlaceRepository;
import com.planit.domain.user.entity.User;
import com.planit.domain.user.repository.UserRepository;
import com.planit.global.common.exception.BusinessException;
import com.planit.global.common.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final TripThemeRepository tripThemeRepository;
    private final WantedPlaceRepository wantedPlaceRepository;
    private final ItineraryDayRepository itineraryDayRepository;
    private final ItineraryItemPlaceRepository itineraryItemPlaceRepository;
    private final ItineraryItemTransportRepository itineraryItemTransportRepository;
    private final AiItineraryQueue aiItineraryQueue;
    private final AiItineraryProcessor aiItineraryProcessor;
    private final boolean aiMockEnabled;

    public TripService(
            TripRepository tripRepository,
            UserRepository userRepository,
            TripThemeRepository tripThemeRepository,
            WantedPlaceRepository wantedPlaceRepository,
            ItineraryDayRepository itineraryDayRepository,
            ItineraryItemPlaceRepository itineraryItemPlaceRepository,
            ItineraryItemTransportRepository itineraryItemTransportRepository,
            AiItineraryQueue aiItineraryQueue,
            AiItineraryProcessor aiItineraryProcessor,
            @Value("${ai.mock-enabled:false}") boolean aiMockEnabled
    ) {
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.tripThemeRepository = tripThemeRepository;
        this.wantedPlaceRepository = wantedPlaceRepository;
        this.itineraryDayRepository = itineraryDayRepository;
        this.itineraryItemPlaceRepository = itineraryItemPlaceRepository;
        this.itineraryItemTransportRepository = itineraryItemTransportRepository;
        this.aiItineraryQueue = aiItineraryQueue;
        this.aiItineraryProcessor = aiItineraryProcessor;
        this.aiMockEnabled = aiMockEnabled;
    }

    @Transactional
    public Long createTrip(TripCreateRequest request, String loginId) {
        // 1) 토큰에서 추출된 loginId로 사용자 조회
        User user = userRepository.findByLoginIdAndDeletedFalse(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        // 2) 이미 여행이 있으면 생성 막기 (중복 데이터는 정리)
        long existingTripCount = tripRepository.countByUserId(user.getId());
        if (existingTripCount >= 1) {
            // 이미 1개 이상 존재하면 생성은 막고, 중복 데이터가 있으면 정리
            if (existingTripCount >= 2) {
                cleanupDuplicateTrips(user.getId());
            }
            throw new BusinessException(ErrorCode.TRIP_002);
        }

        // 3) 여행 기본 정보 저장
        Trip trip = tripRepository.save(new Trip(
                user,
                request.title(),
                request.arrivalDate(),
                request.departureDate(),
                request.arrivalTime(),
                request.departureTime(),
                request.travelCity(),
                request.totalBudget()
        ));

        // 테마 저장 (Trip 1 : Theme N)
        for (String theme : request.travelTheme()) {
            trip.addTheme(new TripTheme(trip, theme));
        }
        tripRepository.save(trip);

        // 희망 장소 저장
        if (request.wantedPlace() != null) {
            for (String placeId : request.wantedPlace()) {
                wantedPlaceRepository.save(new WantedPlace(trip, placeId));
            }
        }

        // 4) AI 요청을 큐에 적재 (mock 모드면 즉시 처리)
        AiItineraryJob job = new AiItineraryJob(new AiItineraryRequest(
                trip.getId(),
                trip.getArrivalDate(),
                trip.getArrivalTime(),
                trip.getDepartureDate(),
                trip.getDepartureTime(),
                trip.getTravelCity(),
                trip.getTotalBudget(),
                request.travelTheme(),
                request.wantedPlace()
        ));
        if (aiMockEnabled) {
            System.out.println("AI mock 모드: 즉시 일정 생성 처리");
            aiItineraryProcessor.process(job);
        } else {
            aiItineraryQueue.enqueue(job);
        }

        return trip.getId();
    }

    @Transactional
    public void deleteUserTrip(String loginId) {
        // 1) 사용자 조회
        User user = userRepository.findByLoginIdAndDeletedFalse(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        // 2) 유일한 여행 조회 후 삭제
        Trip trip = tripRepository.findTopByUserIdOrderByIdDesc(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_001));

        deleteTripData(trip.getId());
        tripRepository.delete(trip);
    }

    @Transactional
    public Optional<Trip> findOrCleanupUserTrip(String loginId) {
        // 유저의 여행이 2개 이상인 경우, 최신 1개만 유지
        User user = userRepository.findByLoginIdAndDeletedFalse(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        List<Trip> trips = tripRepository.findByUserIdOrderByIdDesc(user.getId());
        if (trips.isEmpty()) {
            return Optional.empty();
        }

        if (trips.size() >= 2) {
            // 최신 1개만 남기고 나머지 정리
            Trip latest = trips.get(0);
            for (int i = 1; i < trips.size(); i++) {
                Trip oldTrip = trips.get(i);
                deleteTripData(oldTrip.getId());
                tripRepository.delete(oldTrip);
            }
            return Optional.of(latest);
        }

        return Optional.of(trips.get(0));
    }

    private void cleanupDuplicateTrips(Long userId) {
        List<Trip> trips = tripRepository.findByUserIdOrderByIdDesc(userId);
        if (trips.size() <= 1) {
            return;
        }
        Trip latest = trips.get(0);
        for (int i = 1; i < trips.size(); i++) {
            Trip oldTrip = trips.get(i);
            deleteTripData(oldTrip.getId());
            tripRepository.delete(oldTrip);
        }
    }

    private void deleteTripData(Long tripId) {
        // 일정 데이터 삭제 (장소/이동 -> 일별 일정 순서)
        List<ItineraryDay> days = itineraryDayRepository.findByTripId(tripId);
        List<Long> dayIds = days.stream().map(ItineraryDay::getId).toList();
        if (!dayIds.isEmpty()) {
            itineraryItemPlaceRepository.deleteByItineraryDayIdIn(dayIds);
            itineraryItemTransportRepository.deleteByItineraryDayIdIn(dayIds);
        }
        itineraryDayRepository.deleteAll(days);

        // 테마/희망장소 삭제
        tripThemeRepository.deleteByTripId(tripId);
        wantedPlaceRepository.deleteByTripId(tripId);
    }

}
