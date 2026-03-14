package com.planit.domain.trip.repository;

import com.planit.domain.trip.entity.TripTheme;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripThemeRepository extends JpaRepository<TripTheme, Long> {
    @Modifying
    @Query("delete from TripTheme theme where theme.trip.id = :tripId")
    void deleteByTripId(@Param("tripId") Long tripId);
}
