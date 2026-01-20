package com.planit.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserAvailabilityResponse {
    private final boolean available;
    private final String message;
}
