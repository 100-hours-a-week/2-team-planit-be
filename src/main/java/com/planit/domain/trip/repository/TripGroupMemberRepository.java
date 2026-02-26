package com.planit.domain.trip.repository;

import com.planit.domain.trip.entity.TripGroupMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripGroupMemberRepository extends JpaRepository<TripGroupMember, Long> {
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);
    long countByGroupIdAndSubmittedTrue(Long groupId);
    List<TripGroupMember> findByGroupIdAndSubmittedTrue(Long groupId);
    Optional<TripGroupMember> findByGroupIdAndUserId(Long groupId, Long userId);
}
