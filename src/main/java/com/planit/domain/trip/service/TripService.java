package com.planit.domain.trip.service;

import com.planit.domain.trip.dto.AiItineraryRequest;
import com.planit.domain.trip.dto.TripCreateRequest;
import com.planit.domain.trip.dto.TripListResponse;
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
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
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
    private final boolean createWindowEnabled;

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
            @Value("${ai.mock-enabled:false}") boolean aiMockEnabled,
            @Value("${trip.create-window-enabled:true}") boolean createWindowEnabled
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
        this.createWindowEnabled = createWindowEnabled;
    }

    @Transactional
    public Long createTrip(TripCreateRequest request, String loginId) {
        // 일정 생성 허용 시간(14:00 ~ 다음날 02:00) 외에는 요청 차단
        if (createWindowEnabled && !isCreateWindowOpen(ZonedDateTime.now(ZoneId.of("Asia/Seoul")))) {
            throw new BusinessException(ErrorCode.TRIP_005);
        }

        // 1) 토큰에서 추출된 loginId로 사용자 조회
        User user = userRepository.findByLoginIdAndDeletedFalse(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        // 2) 여행 기본 정보 저장
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

    private boolean isCreateWindowOpen(ZonedDateTime now) {
        LocalTime currentTime = now.toLocalTime();
        LocalTime start = LocalTime.of(14, 0);
        LocalTime end = LocalTime.of(2, 0);
        // 자정을 넘기는 허용 구간: 14:00 이상 또는 02:00 미만
        return !currentTime.isBefore(start) || currentTime.isBefore(end);
    }

    @Transactional
    public void deleteTrip(String loginId, Long tripId) {
        User user = userRepository.findByLoginIdAndDeletedFalse(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_001));
        if (!trip.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.TRIP_006);
        }

        deleteTripData(tripId);
        tripRepository.delete(trip);
    }

    @Transactional(readOnly = true)
    public TripListResponse getUserTrips(String loginId) {
        User user = userRepository.findByLoginIdAndDeletedFalse(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        List<TripListResponse.TripSummary> summaries = tripRepository.findByUserIdOrderByIdDesc(user.getId())
                .stream()
                .map(trip -> new TripListResponse.TripSummary(
                        trip.getId(),
                        trip.getTitle(),
                        trip.getArrivalDate(),
                        trip.getDepartureDate(),
                        trip.getTravelCity()
                ))
                .collect(Collectors.toList());

        return new TripListResponse(summaries);
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
