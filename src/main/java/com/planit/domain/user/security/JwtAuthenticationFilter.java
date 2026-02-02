package com.planit.domain.user.security; // JWT 인증/필터 관련 패키지

import com.planit.domain.user.security.JwtProvider; // JWT 생성/검증 유틸
import com.planit.domain.user.repository.UserRepository; // 사용자 조회용 리포지토리
import jakarta.servlet.FilterChain; // 서블릿 필터 체인
import jakarta.servlet.ServletException; // 서블릿 예외
import jakarta.servlet.http.HttpServletRequest; // 요청 객체
import jakarta.servlet.http.HttpServletResponse; // 응답 객체
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger; // 로그 인터페이스
import org.slf4j.LoggerFactory; // 로그팩토리
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // 인증 객체
import org.springframework.security.core.context.SecurityContextHolder; // SecurityContextHolder 접근
import org.springframework.security.core.userdetails.User; // Spring Security User
import org.springframework.security.core.userdetails.UserDetails; // UserDetails 인터페이스
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource; // 인증 세부정보 생성기
import org.springframework.stereotype.Component; // Bean 등록
import org.springframework.util.StringUtils; // 문자열 유틸
import org.springframework.web.filter.OncePerRequestFilter; // 단일 실행 필터

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider; // JWT 생성/검증 helper
    private final UserRepository userRepository; // 로그인 ID로 사용자 조회
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class); // 필터 로그

    public JwtAuthenticationFilter(JwtProvider jwtProvider, UserRepository userRepository) {
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        return uri.equals("/auth/login")
            || uri.startsWith("/users/signup")
            || uri.startsWith("/users/check-login-id")
            || uri.startsWith("/users/check-nickname")
            || uri.startsWith("/health")
            || uri.startsWith("/swagger-ui")
            || uri.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {

        log.info("JWT FILTER HIT: {}", request.getRequestURI()); // 필터 진입 로그

        String token = resolveToken(request);
        log.info("Resolved JWT token: {} ", token);
        if (StringUtils.hasText(token) && jwtProvider.validateToken(token)) { // 토큰 존재 & 유효성 확인
            log.info("JWT valid result: true");
            String loginId = jwtProvider.getSubject(token); // 토큰에서 loginId 추출
            log.info("JWT subject: {}", loginId);
            Optional<com.planit.domain.user.entity.User> optional = userRepository.findByLoginIdAndDeletedFalse(loginId);
            if (optional.isPresent()) {
                com.planit.domain.user.entity.User user = optional.get();
                UserDetails userDetails = User.withUsername(user.getLoginId())
                    .password(user.getPassword())
                    .authorities("ROLE_USER")
                    .build(); // UserDetails 객체 생성

                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()); // 인증 객체
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request)); // 세부 정보 추가
                SecurityContextHolder.getContext().setAuthentication(authentication); // SecurityContext에 등록
            }
        } else {
            boolean hasText = StringUtils.hasText(token);
            log.info("JWT valid result: false (hasText={} , provided token={})", hasText, token);
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) { // Authorization 헤더에서 Bearer token 추출
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
