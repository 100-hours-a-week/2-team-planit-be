package com.planit.domain.trip.repository;

import com.planit.domain.trip.entity.TripGroup;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripGroupRepository extends JpaRepository<TripGroup, Long> {
    Optional<TripGroup> findByTripId(Long tripId);
    Optional<TripGroup> findByInviteCode(String inviteCode);
}
