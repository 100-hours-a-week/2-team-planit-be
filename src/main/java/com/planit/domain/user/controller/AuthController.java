package com.planit.domain.user.controller; // 인증 관련 컨트롤러 패키지입니다.

import com.planit.domain.user.dto.LoginRequest; // 로그인 요청 payload를 바인딩하는 DTO
import com.planit.domain.user.dto.LoginResponse; // JWT 발급 결과를 담는 DTO
import com.planit.domain.user.service.AuthService; // 로그인/인증 로직을 수행하는 서비스
import jakarta.validation.Valid; // 요청 유효성 검사를 위한 표준 애노테이션
import lombok.RequiredArgsConstructor; // 생성자 자동 주입을 위한 Lombok 애노테이션
import org.springframework.web.bind.annotation.PostMapping; // POST 메서드를 매핑
import org.springframework.web.bind.annotation.RequestBody; // HTTP body를 파라미터로 바인딩
import org.springframework.web.bind.annotation.RequestMapping; // 컨트롤러의 기본 경로를 정의
import org.springframework.web.bind.annotation.ResponseStatus; // 상태 코드 응답 설정
import org.springframework.web.bind.annotation.RestController; // REST 컨트롤러 설정
import org.springframework.http.HttpStatus; // HTTP 상태 코드를 상수화함

@RestController // REST 엔드포인트를 제공하는 컨트롤러임을 선언
@RequestMapping("/auth") // context-path("/api")와 결합돼 경로가 "/api/auth/**"가 됨
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동으로 생성
// 인증 관련 최상단 엔드포인트들을 모아두는 컨트롤러
public class AuthController {

    private final AuthService authService; // 실제 인증/토큰 생성을 담당할 서비스

    @PostMapping("/login") // POST /api/auth/login 엔드포인트
    @ResponseStatus(HttpStatus.OK) // 인증 성공 시 200 상태를 명시
    // 로그인 요청을 받아 AuthService를 호출해 JWT 응답을 반환
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
