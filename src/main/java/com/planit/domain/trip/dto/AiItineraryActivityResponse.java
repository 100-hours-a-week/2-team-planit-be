package com.planit.domain.trip.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalTime;

// AI 응답의 activities[] 요소
public record AiItineraryActivityResponse(
        String type,
        String placeId,
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,
        Integer cost,
        Integer duration,
        String memo
        /*
            "placeName": "면식당",
            "transport":null,
            "type": "Restaurant",
            "eventOrder":1,
            "startTime": "00:00",
            "duration": 120,
            "cost": 10000,
            "memo": "메모 내용"
            "googleMapUrl":"예시url"
         */
) {
}
