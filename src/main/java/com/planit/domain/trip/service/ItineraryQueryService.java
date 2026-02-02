package com.planit.domain.trip.service;

import com.planit.domain.trip.dto.ItineraryActivityResponse;
import com.planit.domain.trip.dto.ItineraryDayResponse;
import com.planit.domain.trip.dto.ItineraryResponse;
import com.planit.domain.trip.entity.ItineraryDay;
import com.planit.domain.trip.entity.ItineraryItemPlace;
import com.planit.domain.trip.entity.ItineraryItemTransport;
import com.planit.domain.trip.entity.Trip;
import com.planit.domain.trip.repository.ItineraryDayRepository;
import com.planit.domain.trip.repository.ItineraryItemPlaceRepository;
import com.planit.domain.trip.repository.ItineraryItemTransportRepository;
import com.planit.domain.trip.repository.TripRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ItineraryQueryService {

    private final TripRepository tripRepository;
    private final ItineraryDayRepository itineraryDayRepository;
    private final ItineraryItemPlaceRepository placeRepository;
    private final ItineraryItemTransportRepository transportRepository;
    private final TripService tripService;

    public ItineraryQueryService(
            TripRepository tripRepository,
            ItineraryDayRepository itineraryDayRepository,
            ItineraryItemPlaceRepository placeRepository,
            ItineraryItemTransportRepository transportRepository,
            TripService tripService
    ) {
        this.tripRepository = tripRepository;
        this.itineraryDayRepository = itineraryDayRepository;
        this.placeRepository = placeRepository;
        this.transportRepository = transportRepository;
        this.tripService = tripService;
    }

    public Optional<ItineraryResponse> getUserItineraries(String loginId) {
        // 토큰 기반으로 유저의 유일한 여행을 조회 (중복 데이터는 최신 1개만 유지)
        Optional<Trip> optionalTrip = tripService.findOrCleanupUserTrip(loginId);
        return optionalTrip.map(trip -> buildItineraries(trip));
    }

    public Optional<ItineraryResponse> getTripItineraries(Long tripId) {
        Trip trip = tripRepository.findById(tripId).orElse(null);
        if (trip == null) {
            return Optional.empty();
        }

        return Optional.of(buildItineraries(trip));
    }

    private ItineraryResponse buildItineraries(Trip trip) {
        // 여행 1건에 대해 일별 일정 + 이벤트를 응답 DTO로 변환
        Long tripId = trip.getId();
        List<ItineraryDay> itineraryDays = itineraryDayRepository.findByTripIdOrderByDayIndex(tripId);
        List<ItineraryDayResponse> dayResponses = new ArrayList<>();

        for (ItineraryDay day : itineraryDays) {
            List<ItineraryActivityResponse> activities = new ArrayList<>();

            List<ItineraryItemPlace> places =
                    placeRepository.findByItineraryDayIdOrderByEventOrder(day.getId());
            for (ItineraryItemPlace place : places) {
                activities.add(new ItineraryActivityResponse(
                        place.getId(),
                        place.getPlaceName(),
                        null,
                        place.getType(),
                        place.getEventOrder(),
                        place.getStartTime(),
                        resolveDurationMinutes(place.getDurationTime()),
                        place.getCost(),
                        place.getMemo(),
                        place.getGoogleMapUrl()
                ));
            }

            List<ItineraryItemTransport> transports =
                    transportRepository.findByItineraryDayIdOrderByEventOrder(day.getId());
            for (ItineraryItemTransport transport : transports) {
                activities.add(new ItineraryActivityResponse(
                        transport.getId(),
                        null,
                        transport.getTransport(),
                        transport.getType(),
                        transport.getEventOrder(),
                        transport.getStartTime(),
                        resolveDurationMinutes(transport.getDurationTime()),
                        null,
                        null,
                        null
                ));
            }

            activities.sort(Comparator.comparing(ItineraryActivityResponse::eventOrder, Comparator.nullsLast(Integer::compareTo)));

            LocalDate date = day.getDate() != null ? day.getDate().toLocalDate()
                    : (trip.getArrivalDate() != null ? trip.getArrivalDate().plusDays(day.getDayIndex() - 1) : null);
            dayResponses.add(new ItineraryDayResponse(day.getId(), day.getDayIndex(), date, activities));
        }

        return new ItineraryResponse(tripId, dayResponses);
    }

    private Integer resolveDurationMinutes(LocalTime durationTime) {
        if (durationTime == null) {
            return null;
        }
        return durationTime.getHour() * 60 + durationTime.getMinute();
    }
}
