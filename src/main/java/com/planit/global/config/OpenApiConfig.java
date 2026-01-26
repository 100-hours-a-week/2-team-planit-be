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
        title = "Planit Backend API", // API명
        version = "v1", // 문서 버전
        description = """
            회원가입/마이페이지/게시물 목록/게시물 작성 기능정의서 기반 API 문서
            게시물 목록/필터는 posts/users/comments/likes/posted_images/post_ranking_snapshots 조합 결과를 반환합니다.
            게시물 생성은 posts/users/posted_images/images/post_places/places/post_trips/trips 관계를 고려하고, 최대 5장 이미지 + 여행/장소 연계를 지원합니다.
            Swagger UI에 Authorize 버튼을 눌러 POST /api/auth/login으로 발급된 Bearer 토큰을 입력하면 보호된 API도 테스트 가능합니다.
            """ // 문서 활용 안내
    ),
    security = @SecurityRequirement(name = "bearerAuth") // 문서 전반에 적용할 보안 요구
)
@SecurityScheme(
    name = "bearerAuth", // Swagger Authorize 버튼 이름
    type = SecuritySchemeType.HTTP, // HTTP 인증 방식
    scheme = "bearer", // Bearer 토큰
    bearerFormat = "JWT" // JWT 형식
)
public class OpenApiConfig {
    // SecurityScheme 정의만 필요한 설정 클래스 (Bean 정의 없음)
}
