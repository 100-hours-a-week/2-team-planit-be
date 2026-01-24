package com.planit.global.config; // 전체 애플리케이션 공통 설정을 두는 패키지

import io.swagger.v3.oas.annotations.OpenAPIDefinition; // OpenAPI 정의 애노테이션
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType; // 스키마 타입 열거
import io.swagger.v3.oas.annotations.info.Info; // API 정보 설명
import io.swagger.v3.oas.annotations.security.SecurityRequirement; // 보안 요구 정의
import io.swagger.v3.oas.annotations.security.SecurityScheme; // 보안 스킴 정의
import org.springframework.context.annotation.Configuration; // 스프링 설정 클래스

@Configuration // Spring 설정 클래스임을 표시
@OpenAPIDefinition(
    info = @Info(
        title = "Planit Backend API", // API 이름
        version = "v1", // API 버전
        description = """
            회원가입/마이페이지/게시물 목록 기능정의서를 기반으로 한 여러 도메인 API
            게시물 목록은 posts/users/comments/likes/posted_images/post_ranking_snapshots 테이블을 조합한 검색/정렬/페이징 결과를 반환하며,
            Swagger UI 우측 상단 Authorize 버튼을 클릭한 뒤 로그인(`POST /api/auth/login`) 결과 accessToken을 `Bearer <token>` 형태로 입력하면
            보호된 경로에서 JWT 인증 테스트도 가능합니다.
            """ // Swagger 활용 안내
    ),
    security = @SecurityRequirement(name = "bearerAuth") // 모든 엔드포인트 기본 보안 요구
)
@SecurityScheme(
    name = "bearerAuth", // Swagger에서 Authorize 버튼이 붙는 스킴 이름
    type = SecuritySchemeType.HTTP, // HTTP 기반 인증
    scheme = "bearer", // Bearer token 사용
    bearerFormat = "JWT" // JWT 토큰 형식 명시
)
public class OpenApiConfig {
    // 빈 정의는 현재 없지만, 보안 정의를 제공하기 위해 유지
}
