package com.planit.domain.user.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TokenReissueResponse {
    private final String accessToken;
    private final String refreshToken;
}
