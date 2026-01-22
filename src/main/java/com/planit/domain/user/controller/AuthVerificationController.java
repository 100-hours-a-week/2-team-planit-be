package com.planit.domain.user.controller; // 인증/토큰 관련 API를 모은 패키지이다.

import com.planit.domain.user.dto.TokenValidationResponse; // 토큰 검증 결과를 담는 DTO
import lombok.RequiredArgsConstructor; // final 필드 주입을 위한 Lombok 애노테이션
import org.springframework.security.core.Authentication; // 인증된 컨텍스트를 받는 파라미터
import org.springframework.web.bind.annotation.GetMapping; // GET 요청 매핑
import org.springframework.web.bind.annotation.RequestMapping; // 컨트롤러 레벨 path 설정
import org.springframework.web.bind.annotation.RestController; // REST 컨트롤러 선언

@RestController // REST 엔드포인트를 처리하는 컨트롤러임을 표시
@RequestMapping("/auth") // "/api" context-path와 합쳐져 전체 경로가 "/api/auth/**"가 됨
@RequiredArgsConstructor // final 필드(현재 없음) 주입 생성을 위한 Lombok
public class AuthVerificationController {

    @GetMapping("/verify") // GET /api/auth/verify로 토큰 유효성을 확인
    public TokenValidationResponse verify(Authentication authentication) {
        // JWT 필터를 통과한 인증 객체에서 loginId를 꺼내어 response로 전달
        return new TokenValidationResponse(authentication.getName(), "*토큰이 유효합니다.");
    }
}
