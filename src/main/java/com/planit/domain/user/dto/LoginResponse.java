package com.planit.domain.user.dto; // 사용자 인증 관련 DTO 패키지

import lombok.Getter; // Getter 자동 생성
import lombok.RequiredArgsConstructor; // final 필드용 생성자 자동 생성

@Getter
@RequiredArgsConstructor
public class LoginResponse {
    private final Long userId; // 인증된 사용자의 PK
    private final String loginId; // 로그인 ID
    private final String nickname; // 사용자 닉네임
    private final String accessToken; // 클라이언트로 반환할 JWT 액세스 토큰
}
