package com.planit.domain.trip.repository;

import com.planit.domain.trip.entity.Trip;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<Trip, Long> {
    long countByUserId(Long userId);
    List<Trip> findByUserIdOrderByIdDesc(Long userId);
    Optional<Trip> findTopByUserIdOrderByIdDesc(Long userId);
}
