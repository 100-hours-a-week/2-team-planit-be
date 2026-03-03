package com.planit.domain.trip.repository;

import com.planit.domain.trip.entity.Trip;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface TripRepository extends JpaRepository<Trip, Long> {
    // owner(user_id) 기준 조회 (기존 단일 소유자 조회 로직)
    List<Trip> findByUserIdOrderByIdDesc(Long userId);
    Optional<Trip> findByIdAndUserId(Long tripId, Long userId);

    @Query(value = """
            SELECT DISTINCT t.*
            FROM trips t
            LEFT JOIN trip_group_members tgm ON tgm.group_id = t.group_id
            WHERE t.user_id = :userId OR tgm.user_id = :userId
            ORDER BY t.id DESC
            """, nativeQuery = true)
    List<Trip> findReadableTripsByUserIdOrderByIdDesc(@Param("userId") Long userId);
}
