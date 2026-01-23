package com.planit.domain.user.security; // JWT 도메인 관련 보안 패키지

import io.jsonwebtoken.Claims; // JWT claims 추출
import io.jsonwebtoken.Jwts; // JWT 빌더/파서
import io.jsonwebtoken.SignatureAlgorithm; // 서명 알고리즘
import io.jsonwebtoken.security.Keys; // HMAC 키 생성
import java.security.Key; // 서명 키 타입
import java.time.Duration; // 토큰 유효시간 표현
import java.time.Instant; // 현재 시각
import java.util.Date; // Date 변환
import lombok.Getter; // getter 자동 생성
import org.springframework.beans.factory.annotation.Value; // 프로퍼티 주입
import org.springframework.stereotype.Component; // 빈 등록

@Component // 스프링 빈으로 등록
public class JwtProvider {

    @Value("${jwt.secret}") // application.yml에서 시크릿 주입
    private String jwtSecret;

    @Getter
    private final Duration accessTokenValidity = Duration.ofMinutes(30); // access token TTL

    public String generateToken(String loginId) { // loginId로 JWT 생성
        Instant now = Instant.now();
        return Jwts.builder()
            .setSubject(loginId)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plus(accessTokenValidity)))
            .signWith(key(), SignatureAlgorithm.HS256) // HS256으로 서명
            .compact();
    }

    private Key key() { // 시크릿을 키 객체로 변환
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public boolean validateToken(String token) { // JWT 유효성 검사
        try {
            Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getSubject(String token) { // JWT subject(loginId) 추출
        return Jwts.parserBuilder()
            .setSigningKey(key())
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }
}
