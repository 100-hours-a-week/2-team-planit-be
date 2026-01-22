package com.planit.domain.user.dto; // 사용자 프로필 응답을 정의한 DTO 패키지

import lombok.Getter; // Getter 자동 생성
import lombok.RequiredArgsConstructor; // final 필드용 생성자 자동 생성

@Getter
@RequiredArgsConstructor
public class UserProfileResponse {
    private final Long userId; // 사용자 ID
    private final String loginId; // 로그인 ID
    private final String nickname; // 사용자 닉네임
}
