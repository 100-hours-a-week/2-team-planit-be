package com.planit.domain.user.service; // 사용자 관련 도메인 서비스를 모아둔 패키지

import com.planit.domain.user.dto.SignUpRequest; // 회원가입 요청 DTO
import com.planit.domain.user.dto.UserAvailabilityResponse; // 중복 확인 응답 DTO
import com.planit.domain.user.dto.UserProfileResponse; // 내 정보 반환용 DTO
import com.planit.domain.user.dto.UserSignupResponse; // 회원가입 결과 DTO
import com.planit.domain.user.dto.UserUpdateRequest; // 내 정보 수정 요청 DTO
import com.planit.domain.user.entity.User; // User 엔티티
import com.planit.domain.user.entity.UserProfileImage; // 프로필 이미지 엔티티
import com.planit.domain.user.repository.UserProfileImageRepository; // 이미지 테이블 저장소
import com.planit.domain.user.repository.UserRepository; // 사용자 테이블 저장소
import java.time.LocalDateTime; // 생성/수정 시간
import java.util.Collections;
import java.util.Optional;
import lombok.RequiredArgsConstructor; // final 필드 생성자 자동 생성
import lombok.extern.slf4j.Slf4j; // 로그 출력
import org.springframework.http.HttpStatus; // HTTP 상태 코드
import org.springframework.security.crypto.password.PasswordEncoder; // 비밀번호 암호화/검증
import org.springframework.stereotype.Service; // 서비스 빈 선언
import org.springframework.transaction.annotation.Transactional; // 트랜잭션 처리
import org.springframework.util.StringUtils; // 문자열 비어있는지 검사
import org.springframework.web.server.ResponseStatusException; // REST 에러 응답

@Service // 사용자 도메인 로직을 담당하는 서비스 빈
@RequiredArgsConstructor // final 필드를 위한 생성자 자동 제공
@Slf4j // 로거 자동 생성
public class UserService {
    private final UserRepository userRepository; // users 테이블 CRUD
    private final UserProfileImageRepository userProfileImageRepository; // user_image 관계 관리
    private final PasswordEncoder passwordEncoder; // BCrypt 암호화 도구

    /**
     * 회원가입 엔드포인트에서 요청받은 정보를 users/user_image 테이블에 저장.
     */
    public UserSignupResponse signup(SignUpRequest request) {
        // 아이디 중복 여부 확인
        if (userRepository.existsByLoginIdAndDeletedFalse(request.getLoginId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*중복된 아이디 입니다.");
        }
        // 닉네임 중복 여부 확인
        if (userRepository.existsByNicknameAndDeletedFalse(request.getNickname())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*중복된 닉네임 입니다.");
        }

        User user = new User(); // 엔티티 생성
        user.setLoginId(request.getLoginId());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // 암호화 저장
        user.setNickname(request.getNickname());
        LocalDateTime now = LocalDateTime.now(); // 현재 시간 기록
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setDeleted(false); // 삭제되지 않음
        User saved = userRepository.save(user); // 사용자 저장

        UserProfileImage profileImage = new UserProfileImage(); // 프로필 이미지 등록
        profileImage.setUserId(saved.getId());
        profileImage.setImageId(request.getProfileImageId());
        userProfileImageRepository.save(profileImage);

        return new UserSignupResponse(saved.getId()); // 생성된 ID 응답
    }

    /**
     * 사용자 프로필 이미지 관계를 제거합니다.
     */
    @Transactional // 트랜잭션 필요 (삭제/조회)
    public void deleteProfileImage(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."));
        if (user.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다.");
        }
        if (!userProfileImageRepository.existsByUserId(userId)) {
            return; // 이미지가 없으면 무시
        }
        userProfileImageRepository.deleteByUserId(userId); // 이미지 삭제
    }

    /**
     * 로그인 ID 중복 여부를 helper text 기준 메시지로 반환합니다.
     */
    public UserAvailabilityResponse checkLoginId(String loginId) {
        if (userRepository.existsByLoginIdAndDeletedFalse(loginId)) {
            return new UserAvailabilityResponse(false, "*중복된 아이디 입니다.");
        }
        return new UserAvailabilityResponse(true, "사용 가능한 아이디 입니다.");
    }

    /**
     * 닉네임 중복 여부를 마이페이지 helper text 기준으로 응답합니다.
     */
    public UserAvailabilityResponse checkNickname(String nickname) {
        if (userRepository.existsByNicknameAndDeletedFalse(nickname)) {
            return new UserAvailabilityResponse(false, "*중복된 닉네임 입니다.");
        }
        return new UserAvailabilityResponse(true, "사용 가능한 닉네임 입니다.");
    }

    /**
     * 인증된 사용자의 프로필(닉네임/이미지/계획 데이터)을 조회합니다.
     */
    public UserProfileResponse getProfile(String loginId) {
        User user = userRepository.findByLoginIdAndDeletedFalse(loginId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."));
        Optional<UserProfileImage> profileImage = userProfileImageRepository.findByUserId(user.getId());
        log.info("Retrieved profile for loginId={}", loginId); // 로그 기록
        return new UserProfileResponse(
            user.getId(),
            user.getLoginId(),
            user.getNickname(),
            profileImage.map(UserProfileImage::getImageId).orElse(null),
            profileImage.isPresent(),
            Collections.emptyList()
        );
    }

    /**
     * 마이페이지 수정 요청을 처리하고, 갱신된 프로필을 반환합니다.
     */
    @Transactional
    public UserProfileResponse updateProfile(String loginId, UserUpdateRequest request) {
        User user = userRepository.findByLoginIdAndDeletedFalse(loginId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."));

        if (StringUtils.hasText(request.getNickname()) && !user.getNickname().equals(request.getNickname())) {
            if (userRepository.existsByNicknameAndDeletedFalse(request.getNickname())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*중복된 닉네임입니다.");
            }
            user.setNickname(request.getNickname());
        }

        if (StringUtils.hasText(request.getPassword())) {
            if (!StringUtils.hasText(request.getPasswordConfirm())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*비밀번호를 한 번 더 입력해주세요.");
            }
            if (!request.getPassword().equals(request.getPasswordConfirm())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*비밀번호와 다릅니다.");
            }
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        } else if (StringUtils.hasText(request.getPasswordConfirm())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*비밀번호를 입력해주세요.");
        }

        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        syncProfileImage(user.getId(), request.getProfileImageId());
        return getProfile(loginId);
    }

    /**
     * 회원 탈퇴(soft delete) 처리 및 연결된 이미지 관계 제거.
     */
    @Transactional
    public void deleteAccount(String loginId) {
        User user = userRepository.findByLoginIdAndDeletedFalse(loginId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."));
        if (user.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다.");
        }
        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
        userProfileImageRepository.deleteByUserId(user.getId());
        log.info("User {} marked as deleted", loginId);
    }

    /**
     * 요청한 이미지 ID로 user_image 관계를 재정렬.
     */
    private void syncProfileImage(Long userId, Long imageId) {
        userProfileImageRepository.deleteByUserId(userId);
        if (imageId == null) {
            return;
        }
        UserProfileImage profileImage = new UserProfileImage();
        profileImage.setUserId(userId);
        profileImage.setImageId(imageId);
        userProfileImageRepository.save(profileImage);
    }

}
