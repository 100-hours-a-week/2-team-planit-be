package com.planit.domain.user.service; // 인증/사용자 서비스 패키지

import com.planit.domain.user.dto.LoginRequest; // 로그인 요청 DTO
import com.planit.domain.user.dto.LoginResponse; // 로그인 응답 DTO
import com.planit.domain.user.dto.LogoutRequest;
import com.planit.domain.user.dto.LogoutResponse;
import com.planit.domain.user.dto.TokenReissueRequest;
import com.planit.domain.user.dto.TokenReissueResponse;
import com.planit.domain.user.entity.User; // 유저 엔티티
import com.planit.domain.user.repository.UserRepository; // 사용자 조회용 레포지토리
import com.planit.domain.user.security.JwtProvider; // JWT 생성/검증 유틸
import com.planit.global.common.exception.BusinessException;
import com.planit.global.common.exception.ErrorCode;
import com.planit.infrastructure.storage.S3ImageUrlResolver;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid; // 요청 유효성 검증
import lombok.RequiredArgsConstructor; // final 필드 생성자 자동화
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus; // HTTP 상태 코드
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.security.crypto.password.PasswordEncoder; // 비밀번호 암호화/검증
import org.springframework.stereotype.Service; // 서비스 빈 선언
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException; // 예외 반환

@Service // 서비스 계층 빈
@RequiredArgsConstructor // final 필드 생성자 자동 생성
public class AuthService {
    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh:token:";
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository; // 사용자 조회
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder; // 패스워드 비교
    private final JwtProvider jwtProvider; // JWT 생성
    private final S3ImageUrlResolver imageUrlResolver;
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;
    @Value("${spring.data.redis.port:6380}")
    private int redisPort;

    @PostConstruct
    void logAuthServiceBeanInfo() {
        log.info("AuthService initialized - class={}, instanceHash={}, redisTemplateClass={}, redisHost={}, redisPort={}",
            this.getClass().getName(),
            System.identityHashCode(this),
            redisTemplate.getClass().getName(),
            redisHost,
            redisPort
        );
        try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
            log.info("AuthService redis factory check - connectionFactory={}, ping={}",
                redisTemplate.getConnectionFactory().getClass().getName(),
                connection.ping()
            );
        } catch (Exception e) {
            log.error("AuthService redis factory check failed", e);
        }
    }

    public LoginResponse login(@Valid LoginRequest request) {
        log.info("AuthService.login called - class={}, instanceHash={}",
            this.getClass().getName(), System.identityHashCode(this));
        // 1) 사용자 검증
        User user = userRepository.findByLoginIdAndDeletedFalse(request.getLoginId())
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw credentialsInvalid();
        }

        // 2) access token 생성
        String accessToken = jwtProvider.generateAccessToken(user.getLoginId());
        // 3) refresh token 생성
        String refreshToken = jwtProvider.generateRefreshToken(user.getLoginId());

        // 4) Redis 저장 (저장 실패 시 예외 발생 -> 아래 response 반환 불가)
        upsertRefreshToken(user, refreshToken);

        // 5) response 반환
        String profileImageUrl = imageUrlResolver.resolve(user.getProfileImageKey());
        LoginResponse response = new LoginResponse(
            user.getId(),
            user.getLoginId(),
            user.getNickname(),
            accessToken,
            refreshToken,
            profileImageUrl
        );
        return response;
    }

    @Transactional
    public TokenReissueResponse reissue(@Valid TokenReissueRequest request) {
        // 현재 구현은 refresh token을 body로 전달한다.
        // 운영에서는 HttpOnly Cookie를 쓰면 XSS 방어 측면에서 더 안전하다.
        String rawRefreshToken = validateRefreshToken(request);
        User user = getUserFromToken(rawRefreshToken);
        verifyStoredToken(user, rawRefreshToken);
        return issueNewTokens(user);
    }

    @Transactional
    public LogoutResponse logout(@Valid LogoutRequest request) {
        // 로그아웃은 idempotent 하게 처리하여 토큰이 이미 만료됐어도 200을 반환한다.
        // key 전략이 userId 기반이므로 토큰에서 user를 복원해 해당 키만 제거한다.
        String rawRefreshToken = request.getRefreshToken();
        if (jwtProvider.validateRefreshToken(rawRefreshToken)) {
            User user = getUserFromToken(rawRefreshToken);
            redisTemplate.delete(buildRefreshTokenKey(user.getId()));
        }
        return new LogoutResponse("로그아웃 되었습니다.");
    }

    private String validateRefreshToken(TokenReissueRequest request) {
        String rawRefreshToken = request.getRefreshToken();
        if (!jwtProvider.validateRefreshToken(rawRefreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        return rawRefreshToken;
    }

    private User getUserFromToken(String rawRefreshToken) {
        String loginId = jwtProvider.getSubject(rawRefreshToken);
        return userRepository.findByLoginIdAndDeletedFalse(loginId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));
    }

    private void verifyStoredToken(User user, String rawRefreshToken) {
        String savedToken = redisTemplate.opsForValue().get(buildRefreshTokenKey(user.getId()));
        // 재사용 공격 방지 핵심:
        // 매번 rotate 하므로 Redis의 "현재 토큰"과 정확히 같은 값만 1회성으로 인정한다.
        if (!StringUtils.hasText(savedToken) || !savedToken.equals(rawRefreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    private TokenReissueResponse issueNewTokens(User user) {
        String newAccessToken = jwtProvider.generateAccessToken(user.getLoginId());
        String newRefreshToken = jwtProvider.generateRefreshToken(user.getLoginId());
        // rotate로 이전 refresh token을 즉시 무효화해 토큰 재사용을 차단한다.
        redisTemplate.opsForValue().set(
            buildRefreshTokenKey(user.getId()),
            newRefreshToken,
            jwtProvider.getRefreshTokenValidity()
        );
        return new TokenReissueResponse(newAccessToken, newRefreshToken);
    }

    private void upsertRefreshToken(User user, String refreshToken) {
        // 유저당 refresh token 1개만 유지해 세션 상태를 단순화하고 강제 로그아웃 제어를 쉽게 한다.
        String key = buildRefreshTokenKey(user.getId());
        log.info("Redis 저장 시작 - host={}, port={}, key={}", redisHost, redisPort, key);
        redisTemplate.opsForValue().set(
            key,
            refreshToken,
            jwtProvider.getRefreshTokenValidity()
        );
        String stored = redisTemplate.opsForValue().get(key);
        if (!refreshToken.equals(stored)) {
            log.error("Redis 저장 검증 실패 - key={}, expected={}, actual={}", key, refreshToken, stored);
            throw new IllegalStateException("Refresh token redis save verification failed");
        }
        log.info("Redis 저장 완료 - key={}", key);
    }

    private String buildRefreshTokenKey(Long userId) {
        return REFRESH_TOKEN_KEY_PREFIX + userId;
    }

    private ResponseStatusException credentialsInvalid() {
        return new ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "*아이디 또는 비밀번호를 확인해주세요"
        );
    }

}
