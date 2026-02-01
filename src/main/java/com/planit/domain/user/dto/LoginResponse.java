package com.planit.domain.user.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class LoginResponse {
    private final Long userId;
    private final String loginId;
    private final String nickname;
    private final String accessToken;
    private final String profileImageUrl;
}
