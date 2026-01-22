package com.planit.domain.user.dto; // 사용자 중복확인 응답 DTO 패키지

import lombok.Getter; // Getter 자동 생성
import lombok.RequiredArgsConstructor; // final 필드용 생성자 자동 생성

@Getter
@RequiredArgsConstructor
public class UserAvailabilityResponse {
    private final boolean available; // 요청한 아이디/닉네임 사용 가능 여부
    private final String message; // 메시지(사용 가능/중복 안내)
}
