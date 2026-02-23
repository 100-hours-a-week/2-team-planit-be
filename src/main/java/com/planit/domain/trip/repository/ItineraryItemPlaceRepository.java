package com.planit.domain.trip.repository;

import com.planit.domain.trip.entity.ItineraryItemPlace;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItineraryItemPlaceRepository extends JpaRepository<ItineraryItemPlace, Long> {
    List<ItineraryItemPlace> findByItineraryDayIdOrderByEventOrder(Long itineraryDayId);
    void deleteByItineraryDayIdIn(Collection<Long> itineraryDayIds);
    java.util.Optional<ItineraryItemPlace> findByIdAndItineraryDayId(Long id, Long itineraryDayId);

    @Query("""
        select ip.itineraryDay.trip.id as tripId, ip.placeId as placeId
        from ItineraryItemPlace ip
        where ip.itineraryDay.trip.id in :tripIds
        order by ip.itineraryDay.trip.id asc, ip.itineraryDay.dayIndex asc, ip.eventOrder asc
        """)
    List<TripPlaceInfo> findFirstPlacesByTripIds(@Param("tripIds") List<Long> tripIds);

    interface TripPlaceInfo {
        Long getTripId();
        Long getPlaceId();
    }
}
