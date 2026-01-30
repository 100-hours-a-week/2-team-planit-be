package com.planit.domain.user.service; // 사용자 도메인 비즈니스 로직을 담은 패키지입니다.

import com.planit.domain.post.repository.PostRepository;
import com.planit.domain.user.dto.MyPageResponse;
import com.planit.domain.user.dto.PlanPreview;
import com.planit.domain.user.dto.SignUpRequest; // 회원가입 요청 DTO
import com.planit.domain.user.dto.UserAvailabilityResponse; // 중복 확인 응답 DTO
import com.planit.domain.user.dto.UserProfileResponse; // 내 정보/마이페이지 응답 DTO
import com.planit.domain.user.dto.UserSignupResponse; // 회원가입 결과 DTO
import com.planit.domain.user.dto.UserUpdateRequest; // 회원 정보 수정 요청 DTO
import com.planit.domain.user.entity.User; // 사용자 엔티티
import com.planit.domain.user.entity.UserProfileImage; // 프로필 이미지 엔티티
import com.planit.domain.user.repository.UserProfileImageRepository; // 이미지 테이블 저장소
import com.planit.domain.user.repository.UserRepository; // 사용자 테이블 저장소
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import lombok.RequiredArgsConstructor; // final 필드 생성자 자동 생성
import lombok.extern.slf4j.Slf4j; // 로깅
import org.springframework.http.HttpStatus; // HTTP 상태 코드
import org.springframework.security.crypto.password.PasswordEncoder; // 비밀번호 암호화
import org.springframework.stereotype.Service; // 서비스 빈 선언
import org.springframework.transaction.annotation.Transactional; // 트랜잭션 처리
import org.springframework.web.server.ResponseStatusException; // REST 예외 응답

@Service // Spring 컨테이너에 서비스 빈으로 등록
@RequiredArgsConstructor // final 필드 생성자 자동 생성
@Slf4j // 로깅을 위한 lombok 로거 생성
public class UserService {

    private static final Set<Character.UnicodeBlock> EMOJI_BLOCKS =
        Set.of(
            Character.UnicodeBlock.EMOTICONS,
            Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS,
            Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS,
            Character.UnicodeBlock.DINGBATS,
            Character.UnicodeBlock.TRANSPORT_AND_MAP_SYMBOLS,
            Character.UnicodeBlock.SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS
        ); // 이모지 판단에 사용하는 블록 집합

    private static final long DEFAULT_PROFILE_IMAGE_ID = 1L;

    private final UserRepository userRepository;
    private final UserProfileImageRepository userProfileImageRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;

    public UserSignupResponse signup(SignUpRequest request) {
        // 로그인 아이디 중복 확인
        if (userRepository.existsByLoginIdAndDeletedFalse(request.getLoginId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*중복된 아이디 입니다.");
        }
        // 닉네임 중복 체크
        if (userRepository.existsByNicknameAndDeletedFalse(request.getNickname())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*중복된 닉네임 입니다.");
        }
        // 엔티티 생성 및 필수 필드 할당
        User user = new User();
        user.setLoginId(request.getLoginId());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setDeleted(false);
        User saved = userRepository.save(user);
        // 프로필 이미지 테이블에 매핑
        Long profileImageId = request.getProfileImageId() != null ? request.getProfileImageId() : DEFAULT_PROFILE_IMAGE_ID;
        UserProfileImage profileImage = new UserProfileImage();
        profileImage.setUserId(saved.getId());
        profileImage.setImageId(profileImageId);
        userProfileImageRepository.save(profileImage);
        // 생성된 사용자 ID 응답
        return new UserSignupResponse(saved.getId());
    }

    @Transactional
    public UserProfileResponse updateProfile(String loginId, UserUpdateRequest request) {
        // 인증된 loginId로 현재 사용자 엔티티 조회
        User user = userRepository.findByLoginIdAndDeletedFalse(loginId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."));
        // 닉네임 유효성 및 중복 여부 검증
        ensureNicknameAvailable(request.getNickname(), user);
        String providedPassword = request.getPassword();
        String confirmation = request.getPasswordConfirmation();
        // 비밀번호 변경 시 유효성 확인
        ensurePasswordRules(providedPassword, confirmation);
        user.setNickname(request.getNickname());
        if (providedPassword != null && !providedPassword.isBlank()) {
            user.setPassword(passwordEncoder.encode(providedPassword));
        }
        user.setUpdatedAt(LocalDateTime.now());
        // 프로필 이미지 변경 반영
        applyProfileImage(request.getProfileImageId(), user.getId());
        // 최신 정보 반환
        return buildUserProfileResponse(user);
    }

    public UserAvailabilityResponse checkLoginId(String loginId) {
        // 로그인 ID 중복 시 적절한 메시지 반환
        if (userRepository.existsByLoginIdAndDeletedFalse(loginId)) {
            return new UserAvailabilityResponse(false, "*중복된 아이디 입니다.");
        }
        return new UserAvailabilityResponse(true, "사용 가능한 아이디 입니다.");
    }

    public UserAvailabilityResponse checkNickname(String nickname) {
        // 닉네임 중복 여부 확인
        if (userRepository.existsByNicknameAndDeletedFalse(nickname)) {
            return new UserAvailabilityResponse(false, "*중복된 닉네임 입니다.");
        }
        return new UserAvailabilityResponse(true, "사용 가능한 닉네임 입니다.");
    }

    public UserProfileResponse getProfile(String loginId) {
        User user = userRepository.findByLoginIdAndDeletedFalse(loginId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."));
        log.info("Retrieved profile for loginId={}", loginId);
        // 응답 DTO 생성
        return buildUserProfileResponse(user);
    }

    @Transactional
    public void deleteProfileImage(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."));
        if (user.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다.");
        }
        Optional<UserProfileImage> existing = userProfileImageRepository.findByUserId(userId);
        existing.ifPresent(image -> userProfileImageRepository.delete(image));
    }

    private void ensureNicknameAvailable(String candidate, User currentUser) {
        // 닉네임이 변경 되었고 중복이라면 에러
        if (!candidate.equals(currentUser.getNickname()) &&
            userRepository.existsByNicknameAndDeletedFalse(candidate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*중복된 닉네임입니다.");
        }
        // 이모지 포함 여부 확인
        if (containsEmoji(candidate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*닉네임에는 이모지를 사용할 수 없습니다.");
        }
    }

    private void ensurePasswordRules(String password, String confirmation) {
        boolean hasPassword = password != null && !password.isBlank();
        boolean hasConfirmation = confirmation != null && !confirmation.isBlank();
        if (!hasPassword && !hasConfirmation) {
            return;
        }
        // 비밀번호가 누락되었는지 확인
        if (!hasPassword) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*비밀번호를 입력해주세요.");
        }
        // 확인 비밀번호 누락 시
        if (!hasConfirmation) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*비밀번호를 한 번 더 입력해주세요.");
        }
        // 비밀번호와 확인 입력이 불일치하는지 검증
        if (!password.equals(confirmation)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*비밀번호와 다릅니다.");
        }
        // 비밀번호 정책 (대소문자/숫자/특수문자 포함, 8~20자) 체크
        if (password.length() < 8 || password.length() > 20 ||
            !password.matches(".*[A-Z].*") ||
            !password.matches(".*[a-z].*") ||
            !password.matches(".*\\d.*") ||
            password.chars().noneMatch(ch -> !Character.isLetterOrDigit(ch))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "*비밀번호는 8자 이상, 20자 이하이며, 대문자, 소문자, 숫자, 특수문자를 각각 최소 1개 포함해야 합니다.");
        }
    }

    private void applyProfileImage(Long imageId, Long userId) {
        // 프로필 이미지 ID가 없으면 변경 작업 없음
        if (imageId == null) {
            return;
        }
        Optional<UserProfileImage> existing = userProfileImageRepository.findByUserId(userId);
        if (existing.isPresent()) {
            // 기존 객체에 새로운 ID 설정
            existing.get().setImageId(imageId);
            return;
        }
        // 새 이미지를 등록
        UserProfileImage profileImage = new UserProfileImage();
        profileImage.setUserId(userId);
        profileImage.setImageId(imageId);
        userProfileImageRepository.save(profileImage);
    }

    private UserProfileResponse buildUserProfileResponse(User user) {
        Long profileImageId = userProfileImageRepository.findByUserId(user.getId())
            .map(UserProfileImage::getImageId)
            .orElse(null);
        return new UserProfileResponse(user.getId(), user.getLoginId(), user.getNickname(), profileImageId);
    }

    public MyPageResponse getMyPage(String loginId) {
        User user = userRepository.findByLoginIdAndDeletedFalse(loginId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."));
        UserRepository.ProfileSummary summary = userRepository.fetchProfileSummary(user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "프로필 정보를 불러오지 못했습니다."));
        PageRequest pageRequest = PageRequest.of(0, 3);
        List<PlanPreview> previews = postRepository
            .findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(user.getId(), pageRequest)
            .stream()
            .map(p -> new PlanPreview(
                p.getId(),
                p.getTitle(),
                "내 계획",
                p.getBoardType().name()
            ))
            .toList();
        return new MyPageResponse(
            summary.getUserId(),
            summary.getLoginId(),
            summary.getNickname(),
            summary.getProfileImageId(),
            summary.getPostCount(),
            summary.getCommentCount(),
            summary.getLikeCount(),
            summary.getNotificationCount(),
            previews
        );
    }

    @Transactional
    public void deleteAccount(String loginId) {
        User user = userRepository.findByLoginIdAndDeletedFalse(loginId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."));
        userProfileImageRepository.deleteByUserId(user.getId());
        userRepository.softDelete(user.getId(), LocalDateTime.now());
    }

    private boolean containsEmoji(String value) {
        // UnicodeBlock 정보를 통해 이모지 블록 포함 여부 판단
        return value.codePoints()
            .mapToObj(Character.UnicodeBlock::of)
            .anyMatch(EMOJI_BLOCKS::contains);
    }
}
