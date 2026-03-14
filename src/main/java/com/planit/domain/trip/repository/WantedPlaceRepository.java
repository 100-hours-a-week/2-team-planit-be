package com.planit.domain.trip.repository;

import com.planit.domain.trip.entity.WantedPlace;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WantedPlaceRepository extends JpaRepository<WantedPlace, Long> {
    @Modifying
    @Query("delete from WantedPlace wantedPlace where wantedPlace.trip.id = :tripId")
    void deleteByTripId(@Param("tripId") Long tripId);

    @Query("select wantedPlace from WantedPlace wantedPlace where wantedPlace.trip.id = :tripId")
    List<WantedPlace> findByTripId(@Param("tripId") Long tripId);
}
