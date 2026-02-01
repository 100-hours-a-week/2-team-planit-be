package com.planit.domain.user.service; // 인증/사용자 서비스 패키지

import com.planit.domain.user.config.ProfileImageProperties;
import com.planit.domain.user.dto.LoginRequest; // 로그인 요청 DTO
import com.planit.domain.user.dto.LoginResponse; // 로그인 응답 DTO
import com.planit.domain.user.entity.User; // 유저 엔티티
import com.planit.domain.user.repository.UserRepository; // 사용자 조회용 레포지토리
import com.planit.domain.user.security.JwtProvider; // JWT 생성/검증 유틸
import com.planit.global.common.exception.BusinessException;
import com.planit.global.common.exception.ErrorCode;
import jakarta.validation.Valid; // 요청 유효성 검증
import lombok.RequiredArgsConstructor; // final 필드 생성자 자동화
import org.springframework.http.HttpStatus; // HTTP 상태 코드
import org.springframework.security.crypto.password.PasswordEncoder; // 비밀번호 암호화/검증
import org.springframework.stereotype.Service; // 서비스 빈 선언
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException; // 예외 반환

@Service // 서비스 계층 빈
@RequiredArgsConstructor // final 필드 생성자 자동 생성
public class AuthService {

    private final UserRepository userRepository; // 사용자 조회
    private final PasswordEncoder passwordEncoder; // 패스워드 비교
    private final JwtProvider jwtProvider; // JWT 생성
    private final ProfileImageProperties imageProperties;

    public LoginResponse login(@Valid LoginRequest request) {
        User user = userRepository.findByLoginIdAndDeletedFalse(request.getLoginId())
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw credentialsInvalid();
        }

        String token = jwtProvider.generateToken(user.getLoginId());
        String profileImageUrl = resolveProfileImageUrl(user.getProfileImageUrl());
        return new LoginResponse(
            user.getId(),
            user.getLoginId(),
            user.getNickname(),
            token,
            profileImageUrl
        );
    }

    private ResponseStatusException credentialsInvalid() {
        return new ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "*아이디 또는 비밀번호를 확인해주세요"
        );
    }

    private String resolveProfileImageUrl(String candidate) {
        if (StringUtils.hasText(candidate)) {
            return candidate;
        }
        return imageProperties.getDefaultImageUrl();
    }
}
