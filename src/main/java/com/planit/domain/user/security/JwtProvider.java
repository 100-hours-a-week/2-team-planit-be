package com.planit.domain.user.security;

import com.planit.domain.user.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtProperties jwtProperties;

    private Key key;

    @Getter
    private Duration accessTokenValidity;

    @PostConstruct
    public void init() {
        byte[] keyBytes = decodeSecret(jwtProperties.getSecret());
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret key must be at least 256 bits");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenValidity = Duration.ofMillis(jwtProperties.getAccessTokenExpirationMs());
    }

    public String generateToken(String loginId) {
        Instant now = Instant.now();
        return Jwts.builder()
            .setSubject(loginId)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plus(accessTokenValidity)))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getSubject(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }

    private byte[] decodeSecret(String secret) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("JWT secret must be provided");
        }
        return Decoders.BASE64.decode(secret);
    }
}
