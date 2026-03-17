package com.planit.domain.user.security;

import com.planit.domain.user.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class JwtProvider {
    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private final JwtProperties jwtProperties;

    private Key key;

    @Getter
    private Duration accessTokenValidity;
    @Getter
    private Duration refreshTokenValidity;

    @PostConstruct
    public void init() {
        byte[] keyBytes = decodeSecret(jwtProperties.getSecret());
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenValidity = Duration.ofMillis(jwtProperties.getAccessTokenExpirationMs());
        this.refreshTokenValidity = Duration.ofMillis(jwtProperties.getRefreshTokenExpirationMs());
    }

    public String generateToken(String loginId) {
        return generateAccessToken(loginId);
    }

    public String generateAccessToken(String loginId) {
        return generateToken(loginId, accessTokenValidity, ACCESS_TOKEN_TYPE);
    }

    public String generateRefreshToken(String loginId) {
        return generateToken(loginId, refreshTokenValidity, REFRESH_TOKEN_TYPE);
    }

    public LocalDateTime getRefreshTokenExpiresAt() {
        return LocalDateTime.now().plus(refreshTokenValidity);
    }

    private String generateToken(String loginId, Duration validity, String tokenType) {
        Instant now = Instant.now();
        return Jwts.builder()
            .setSubject(loginId)
            .claim(TOKEN_TYPE_CLAIM, tokenType)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plus(validity)))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    public boolean validateToken(String token) {
        return validateTokenType(token, ACCESS_TOKEN_TYPE);
    }

    public boolean validateRefreshToken(String token) {
        return validateTokenType(token, REFRESH_TOKEN_TYPE);
    }

    private boolean validateTokenType(String token, String expectedType) {
        try {
            String tokenType = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get(TOKEN_TYPE_CLAIM, String.class);
            return expectedType.equals(tokenType);
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
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length >= 32) {
            return raw;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(raw);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }
}
