package com.planit.domain.trip.service;

import com.planit.domain.trip.dto.ItineraryDayUpdateRequest;
import com.planit.domain.trip.entity.ItineraryDay;
import com.planit.domain.trip.entity.ItineraryItemPlace;
import com.planit.domain.trip.repository.ItineraryDayRepository;
import com.planit.domain.trip.repository.ItineraryItemPlaceRepository;
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

    public ItineraryUpdateService(
            ItineraryDayRepository itineraryDayRepository,
            ItineraryItemPlaceRepository placeRepository
    ) {
        this.itineraryDayRepository = itineraryDayRepository;
        this.placeRepository = placeRepository;
    }

    @Transactional
    public void updateDayPlaces(ItineraryDayUpdateRequest request) {
        // 1) 일자 존재 여부 확인
        ItineraryDay day = itineraryDayRepository.findById(request.dayId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_003));

        if (request.places() == null || request.places().isEmpty()) {
            // 수정할 항목이 없으면 그대로 종료
            return;
        }

        // 2) 수정 요청된 장소만 업데이트
        for (ItineraryDayUpdateRequest.PlaceUpdate update : request.places()) {
            ItineraryItemPlace place = placeRepository
                    .findByIdAndItineraryDayId(update.activityId(), day.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_004));

            if (update.placeName() != null) {
                place.updatePlaceName(update.placeName());
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
