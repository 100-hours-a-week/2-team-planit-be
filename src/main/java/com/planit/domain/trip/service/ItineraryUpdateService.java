package com.planit.domain.trip.service;

import com.planit.domain.trip.dto.ItineraryDayUpdateRequest;
import com.planit.domain.trip.entity.ItineraryDay;
import com.planit.domain.trip.entity.ItineraryItemPlace;
import com.planit.domain.trip.repository.ItineraryDayRepository;
import com.planit.domain.trip.repository.ItineraryItemPlaceRepository;
import com.planit.domain.user.entity.User;
import com.planit.domain.user.repository.UserRepository;
import com.planit.global.common.exception.BusinessException;
import com.planit.global.common.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItineraryUpdateService {

    private final ItineraryDayRepository itineraryDayRepository;
    private final ItineraryItemPlaceRepository placeRepository;
    private final UserRepository userRepository;

    public ItineraryUpdateService(
            ItineraryDayRepository itineraryDayRepository,
            ItineraryItemPlaceRepository placeRepository,
            UserRepository userRepository
    ) {
        this.itineraryDayRepository = itineraryDayRepository;
        this.placeRepository = placeRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void updateDayPlaces(ItineraryDayUpdateRequest request, String loginId) {
        // 1) 사용자 조회
        User user = userRepository.findByLoginIdAndDeletedFalse(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        // 2) 일자/여행 관계 확인
        ItineraryDay day = itineraryDayRepository.findByIdAndTripId(request.dayId(), request.tripId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_003));

        // 3) 소유자만 수정 가능
        if (!day.getTrip().getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.TRIP_006);
        }

        if (request.places() == null || request.places().isEmpty()) {
            // 수정할 항목이 없으면 그대로 종료
            return;
        }

        // 4) 수정 요청된 장소만 업데이트
        for (ItineraryDayUpdateRequest.PlaceUpdate update : request.places()) {
            ItineraryItemPlace place = placeRepository
                    .findByIdAndItineraryDayId(update.activityId(), day.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_004));

            if (update.placeName() != null) {
                place.updatePlaceName(update.placeName());
            }
            if (update.placeId() != null) {
                place.updatePlaceId(update.placeId());
            }
            if (update.googleMapUrl() != null) {
                place.updateGoogleMapUrl(update.googleMapUrl());
            }
            if (update.startTime() != null) {
                place.updateStartTime(update.startTime());
            }
            if (update.durationMinutes() != null) {
                place.updateDurationTime(toDurationTime(update.durationMinutes()));
            }
            if (update.cost() != null) {
                place.updateCost(BigDecimal.valueOf(update.cost()));
            }
            if (update.memo() != null) {
                place.updateMemo(update.memo());
            }
        }
    }

    private LocalTime toDurationTime(Integer minutes) {
        int safeMinutes = Math.max(minutes, 0);
        int hours = safeMinutes / 60;
        int remain = safeMinutes % 60;
        return LocalTime.of(hours, remain);
    }
}
