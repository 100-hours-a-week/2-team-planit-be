package com.planit.domain.trip.service;

import com.planit.domain.trip.dto.ItineraryActivityResponse;
import com.planit.domain.trip.dto.ItineraryDayResponse;
import com.planit.domain.trip.dto.ItineraryResponse;
import com.planit.domain.trip.entity.ItineraryItem;
import com.planit.domain.trip.entity.ItineraryItemPlace;
import com.planit.domain.trip.entity.ItineraryItemTransport;
import com.planit.domain.trip.entity.Trip;
import com.planit.domain.trip.repository.ItineraryItemPlaceRepository;
import com.planit.domain.trip.repository.ItineraryItemRepository;
import com.planit.domain.trip.repository.ItineraryItemTransportRepository;
import com.planit.domain.trip.repository.TripRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ItineraryQueryService {

    private final TripRepository tripRepository;
    private final ItineraryItemRepository itineraryItemRepository;
    private final ItineraryItemPlaceRepository placeRepository;
    private final ItineraryItemTransportRepository transportRepository;

    public ItineraryQueryService(
            TripRepository tripRepository,
            ItineraryItemRepository itineraryItemRepository,
            ItineraryItemPlaceRepository placeRepository,
            ItineraryItemTransportRepository transportRepository
    ) {
        this.tripRepository = tripRepository;
        this.itineraryItemRepository = itineraryItemRepository;
        this.placeRepository = placeRepository;
        this.transportRepository = transportRepository;
    }

    public Optional<ItineraryResponse> getTripItineraries(Long tripId) {
        Trip trip = tripRepository.findById(tripId).orElse(null);
        if (trip == null) {
            return Optional.empty();
        }

        List<ItineraryItem> items = itineraryItemRepository.findByTripIdOrderByDayIndex(tripId);
        List<ItineraryDayResponse> days = new ArrayList<>();
        LocalDate baseDate = trip.getArrivalDate();

        for (ItineraryItem item : items) {
            List<ItineraryActivityResponse> activities = new ArrayList<>();

            List<ItineraryItemPlace> places =
                    placeRepository.findByItineraryItemIdOrderByEventOrder(item.getId());
            for (ItineraryItemPlace place : places) {
                activities.add(new ItineraryActivityResponse(
                        "PLACE",
                        place.getEventOrder(),
                        place.getStartTime(),
                        place.getDurationTime(),
                        place.getCost(),
                        null,
                        place.getPlaceId(),
                        place.getPlaceName(),
                        place.getGoogleMapUrl(),
                        place.getPositionLat(),
                        place.getPositionLng()
                ));
            }

            List<ItineraryItemTransport> transports =
                    transportRepository.findByItineraryItemIdOrderByEventOrder(item.getId());
            for (ItineraryItemTransport transport : transports) {
                activities.add(new ItineraryActivityResponse(
                        "ROUTE",
                        transport.getEventOrder(),
                        transport.getStartTime(),
                        transport.getDurationTime(),
                        null,
                        transport.getTransport(),
                        null,
                        null,
                        null,
                        null,
                        null
                ));
            }

            activities.sort(Comparator.comparing(ItineraryActivityResponse::order));

            LocalDate date = baseDate != null ? baseDate.plusDays(item.getDayIndex() - 1) : null;
            days.add(new ItineraryDayResponse(item.getDayIndex(), date, activities));
        }

        return Optional.of(new ItineraryResponse(tripId, days));
    }
}
