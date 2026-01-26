package com.planit.global.security; // 보안 관련 설정 패키지

import com.fasterxml.jackson.databind.ObjectMapper; // JSON 변환 유틸
import com.planit.domain.user.security.JwtAuthenticationFilter; // JWT 필터
import com.planit.global.common.response.ErrorResponse; // 에러 응답 DTO
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor; // final 필드 생성자
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
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

@Configuration // Spring 설정 클래스로 등록
@EnableWebSecurity // Web Security 적용
@RequiredArgsConstructor
public class SecurityConfig {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(); // JSON 변환 유틸
    private final JwtAuthenticationFilter jwtAuthenticationFilter; // 커스텀 JWT 필터

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // JWT 쓰므로 CSRF 불필요
            .httpBasic(basic -> basic.disable()) // HTTP 기본 인증 비활성
            .formLogin(form -> form.disable()) // 폼 로그인 비활성
            .logout(logout -> logout.disable()) // Spring 기본 로그아웃 비활성
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 무상태 세션 정책
            .exceptionHandling(handler -> handler.authenticationEntryPoint(this::handleUnauthenticated)) // 인증 실패 JSON 응답
            .anonymous(Customizer.withDefaults()) // 익명 접근 허용
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/health").permitAll() // health 체크
                .requestMatchers("/healthcheck").permitAll()
                .requestMatchers("/users/signup").permitAll()
                .requestMatchers("/users/check-login-id").permitAll()
                .requestMatchers("/users/check-nickname").permitAll()
                .requestMatchers(HttpMethod.GET, "/posts/**").permitAll() // 게시글은 누구나 조회 가능
                .requestMatchers("/swagger-ui/**").permitAll() // Swagger 접근
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/auth/login").permitAll() // 로그인은 모두 허용
                .anyRequest().authenticated() // 기타 요청은 인증 필요
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // JWT 필터 등록
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // BCrypt 비밀번호 암호화기 반환
    }

    private void handleUnauthenticated(HttpServletRequest request,
                                       HttpServletResponse response,
                                       AuthenticationException exception) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value()); // 401 상태
        response.setContentType(MediaType.APPLICATION_JSON_VALUE); // JSON 응답
        response.setCharacterEncoding(StandardCharsets.UTF_8.name()); // 한글 인코딩
        ErrorResponse errorResponse = ErrorResponse.of("*로그인이 필요한 요청입니다."); // 사용자 메시지
        OBJECT_MAPPER.writeValue(response.getWriter(), errorResponse); // JSON 직렬화
    }
}
