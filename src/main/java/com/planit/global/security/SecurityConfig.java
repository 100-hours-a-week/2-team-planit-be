package com.planit.global.security; // 보안 관련 설정 패키지

import com.fasterxml.jackson.databind.ObjectMapper; // JSON 변환 유틸
import com.planit.domain.user.security.JwtAuthenticationFilter; // JWT 필터
import com.planit.global.common.response.ErrorResponse; // 에러 응답 DTO
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor; // final 필드 생성자
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

@Configuration // Spring 설정 클래스로 등록
@EnableWebSecurity // Web Security 적용
@RequiredArgsConstructor
public class SecurityConfig {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(); // 에러를 JSON으로 변환
    private final JwtAuthenticationFilter jwtAuthenticationFilter; // JWT 필터 주입

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // CSRF 비활성화 (JWT 사용)
            .httpBasic(basic -> basic.disable()) // 기본 인증 비활성화
            .formLogin(form -> form.disable()) // 폼 로그인 비활성화
            .logout(logout -> logout.disable()) // 라이브러리 로그아웃 사용 안 함
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 없이 stateless
            .exceptionHandling(handler -> handler.authenticationEntryPoint(this::handleUnauthenticated)) // 인증 실패 시 JSON 에러
            .anonymous(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/health").permitAll()
                .requestMatchers("/healthcheck").permitAll()
                .requestMatchers("/users/signup").permitAll()
                .requestMatchers("/users/check-login-id").permitAll()
                .requestMatchers("/users/check-nickname").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/auth/login").permitAll()
                .anyRequest().authenticated() // 위 경로 외에는 인증 필요
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // JWT 필터 전단 앞에 등록
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // 비밀번호 암호화 도구
    }

    private void handleUnauthenticated(HttpServletRequest request,
                                       HttpServletResponse response,
                                       AuthenticationException exception) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value()); // 401 상태
        response.setContentType(MediaType.APPLICATION_JSON_VALUE); // JSON body
        ErrorResponse errorResponse = ErrorResponse.of("*로그인이 필요한 요청입니다."); // helper 메시지
        OBJECT_MAPPER.writeValue(response.getWriter(), errorResponse); // 응답 쓰기
    }
}
