package com.planit.domain.trip.service;

import com.planit.domain.trip.entity.Trip;
import com.planit.domain.trip.repository.TripGroupMemberRepository;
import com.planit.domain.user.entity.User;
import com.planit.domain.user.repository.UserRepository;
import com.planit.global.common.exception.BusinessException;
import com.planit.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TripAccessService {

    private final UserRepository userRepository;
    private final TripGroupMemberRepository groupMemberRepository;

    public AccessInfo getAccessInfo(Trip trip, String loginId) {
        User user = userRepository.findByLoginIdAndDeletedFalse(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        boolean isOwner = trip.getUser() != null && trip.getUser().getId().equals(user.getId());
        boolean isMember = isOwner;

        if (!isMember && trip.getGroupId() != null) {
            isMember = groupMemberRepository.existsByGroupIdAndUserId(trip.getGroupId(), user.getId());
        }

        return new AccessInfo(user, isOwner, isMember);
    }

    public AccessInfo requireReadable(Trip trip, String loginId) {
        AccessInfo info = getAccessInfo(trip, loginId);
        if (!info.isMember()) {
            throw new BusinessException(ErrorCode.TRIP_006);
        }
        return info;
    }

    public record AccessInfo(User user, boolean isOwner, boolean isMember) {
    }
}
