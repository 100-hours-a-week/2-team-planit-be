package com.planit.global.security;

import com.fasterxml.jackson.databind.ObjectMapper; // 에러/응답을 JSON으로 직렬화
import com.planit.domain.user.security.JwtAuthenticationFilter; // JWT 검증 흐름을 담당하는 필터
import com.planit.global.common.response.ErrorResponse; // JSON 응답 구조
import jakarta.servlet.http.HttpServletRequest; // 인증 실패/인가 실패 핸들러에서 사용
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor; // final 필드 주입 생성자
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration // 스프링 빈으로 등록하여 security 체인을 설정
@EnableWebSecurity // 기본 Spring Security 구성을 활성화
@RequiredArgsConstructor
public class SecurityConfig {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(); // 에러 바디 직렬화
    private final JwtAuthenticationFilter jwtAuthenticationFilter; // JWT 필터를 security chain 앞단에 연결

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // JWT 기반이라 CSRF 토큰 제거
            .httpBasic(basic -> basic.disable()) // HTTP BASIC 비활성화
            .formLogin(form -> form.disable()) // 폼 로그인 사용할 필요 없음
            .logout(logout -> logout.disable()) // 커스텀 로그아웃 없음
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 유지하지 않음
            .exceptionHandling(handler -> handler
                .authenticationEntryPoint(this::handleUnauthenticated) // 401 응답
                .accessDeniedHandler(this::handleAccessDenied)) // 403 응답
            .anonymous(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/health").permitAll() // 헬스 체크는 인증 없이
                .requestMatchers("/healthcheck").permitAll()
                .requestMatchers("/users/signup").permitAll() // 회원가입/중복 체크는 공개
                .requestMatchers("/users/check-login-id").permitAll()
                .requestMatchers("/users/check-nickname").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll() // Swagger 문서 공개
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/auth/login").permitAll()
                .anyRequest().authenticated() // 마이페이지/수정/탈퇴는 JWT 로그인 요구
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // JWT 인증 필터 우선 등록
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // BCrypt로 패스워드 방어
    }

    private void handleUnauthenticated(HttpServletRequest request,
                                       HttpServletResponse response,
                                       AuthenticationException exception) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value()); // 로그인 필요 401
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse errorResponse = ErrorResponse.of("*로그인이 필요한 요청입니다."); // helper text 그대로
        OBJECT_MAPPER.writeValue(response.getWriter(), errorResponse);
    }

    private void handleAccessDenied(HttpServletRequest request,
                                    HttpServletResponse response,
                                    org.springframework.security.access.AccessDeniedException exception) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value()); // 그룹장 전용/권한 없음 403
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse errorResponse = ErrorResponse.of("*요청 권한이 없습니다.");
        OBJECT_MAPPER.writeValue(response.getWriter(), errorResponse);
    }
}
