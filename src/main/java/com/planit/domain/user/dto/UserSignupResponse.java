package com.planit.domain.user.dto; // 사용자 가입 응답을 담는 DTO 패키지

import lombok.Getter; // Getter 자동 생성
import lombok.RequiredArgsConstructor; // final 필드 생성자 자동 생성

@Getter
@RequiredArgsConstructor
public class UserSignupResponse {
    private final Long userId; // 신규 생성된 사용자 ID
}
