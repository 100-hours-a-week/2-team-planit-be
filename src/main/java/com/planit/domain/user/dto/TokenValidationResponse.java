package com.planit.domain.user.dto; // 인증 토큰 검증 응답을 모아둔 DTO 패키지

import lombok.Getter; // getter 메서드를 자동 생성
import lombok.RequiredArgsConstructor; // final 필드용 생성자 자동 생성

@Getter
@RequiredArgsConstructor
public class TokenValidationResponse {
    private final String loginId; // 검증된 토큰의 주체(loginId)
    private final String message; // 클라이언트에 전달할 helper 메시지
}
