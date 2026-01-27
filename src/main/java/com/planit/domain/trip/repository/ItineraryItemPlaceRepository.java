package com.planit.domain.trip.repository;

import com.planit.domain.trip.entity.ItineraryItemPlace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItineraryItemPlaceRepository extends JpaRepository<ItineraryItemPlace, Long> {
    List<ItineraryItemPlace> findByItineraryItemIdOrderByEventOrder(Long id);
}
