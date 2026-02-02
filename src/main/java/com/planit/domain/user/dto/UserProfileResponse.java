package com.planit.domain.user.dto; // 사용자 프로필/마이페이지 응답을 위한 DTO 패키지

import lombok.Getter; // Getter 자동 생성
import lombok.RequiredArgsConstructor; // final 필드 생성자 자동 생성

@Getter
@RequiredArgsConstructor
public class UserProfileResponse {
    private final Long userId; // 고유한 사용자 식별자
    private final String loginId; // 인증에 사용되는 로그인 ID
    private final String nickname; // 화면에 노출할 닉네임
    private final String profileImageUrl; // 프로필 이미지 URL
}
