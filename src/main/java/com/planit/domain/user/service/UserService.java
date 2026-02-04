package com.planit.domain.user.service; // 사용자 도메인 비즈니스 로직을 담은 패키지입니다.

import com.planit.domain.post.repository.PostRepository;
import com.planit.domain.user.dto.MyPageResponse;
import com.planit.domain.user.dto.PlanPreview;
import com.planit.domain.user.dto.SignUpRequest;
import com.planit.domain.user.dto.UserAvailabilityResponse;
import com.planit.domain.user.dto.UserProfileResponse;
import com.planit.domain.user.dto.UserSignupResponse;
import com.planit.domain.user.dto.UserUpdateRequest;
import com.planit.domain.user.entity.User;
import com.planit.domain.user.exception.DuplicateLoginIdException;
import com.planit.domain.user.exception.DuplicateNicknameException;
import com.planit.domain.user.repository.UserRepository;
import com.planit.domain.user.service.support.UserConstraintMetadata;
import com.planit.global.common.exception.BusinessException;
import com.planit.global.common.exception.ErrorCode;
import com.planit.infrastructure.storage.S3ImageUrlResolver;
import com.planit.infrastructure.storage.S3UploadResult;
import com.planit.infrastructure.storage.S3ImageUrlResolver;
import com.planit.infrastructure.storage.S3UploadResult;
import com.planit.infrastructure.storage.S3Uploader;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final Pattern KEY_PATTERN = Pattern.compile("key '(?:[^.']+\\.)?(?<constraint>[^']+)'");
    private static final Pattern CONSTRAINT_PATTERN = Pattern.compile("constraint '(?<constraint>[^']+)'");

    private static final Set<Character.UnicodeBlock> EMOJI_BLOCKS =
        Set.of(
            Character.UnicodeBlock.EMOTICONS,
            Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS,
            Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS,
            Character.UnicodeBlock.DINGBATS,
            Character.UnicodeBlock.TRANSPORT_AND_MAP_SYMBOLS,
            Character.UnicodeBlock.SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS
        );

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<S3Uploader> s3UploaderProvider;
    private final S3ImageUrlResolver imageUrlResolver;
    private final UserConstraintMetadata constraintMetadata;

    public UserSignupResponse signup(SignUpRequest request) {
        validateLoginId(request.getLoginId());
        validateNickname(request.getNickname());
        User user = new User();
        user.setLoginId(request.getLoginId());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setDeleted(false);
        User saved;
        try {
            saved = userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw resolveDuplicateException(request, ex);
        }
        return new UserSignupResponse(saved.getId());
    }

    private RuntimeException resolveDuplicateException(SignUpRequest request, DataIntegrityViolationException ex) {
        String constraint = findConstraintName(ex);
        if (constraint != null) {
            Optional<String> column = constraintMetadata.findColumnByConstraint(constraint);
            if (column.isPresent()) {
                RuntimeException mapped = mapColumnToException(column.get());
                if (mapped != null) {
                    return mapped;
                }
            }
        }
        if (userRepository.existsByNicknameAndDeletedFalse(request.getNickname())) {
            return new DuplicateNicknameException();
        }
        if (userRepository.existsByLoginIdAndDeletedFalse(request.getLoginId())) {
            return new DuplicateLoginIdException();
        }
        log.error("Unhandled DataIntegrityViolation during signup", ex);
        return new BusinessException(ErrorCode.COMMON_999);
    }

    private RuntimeException mapColumnToException(String column) {
        if ("nickname".equalsIgnoreCase(column)) {
            return new DuplicateNicknameException();
        }
        if ("login_id".equalsIgnoreCase(column)) {
            return new DuplicateLoginIdException();
        }
        return null;
    }

    private String findConstraintName(DataIntegrityViolationException ex) {
        Throwable root = ex.getRootCause();
        while (root != null && !(root instanceof SQLException)) {
            root = root.getCause();
        }
        if (root instanceof SQLException sqlException) {
            return extractConstraintName(sqlException.getMessage());
        }
        return null;
    }

    private String extractConstraintName(String message) {
        if (!StringUtils.hasText(message)) {
            return null;
        }
        Matcher matcher = KEY_PATTERN.matcher(message);
        if (matcher.find()) {
            return matcher.group("constraint");
        }
        matcher = CONSTRAINT_PATTERN.matcher(message);
        if (matcher.find()) {
            return matcher.group("constraint");
        }
        return null;
    }

    @Transactional
    public UserProfileResponse uploadProfileImage(String loginId, MultipartFile file) {
        User user = userRepository.findByLoginIdAndDeletedFalse(loginId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."));
        if (user.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다.");
        }
        S3Uploader uploader = s3UploaderProvider.getIfAvailable();
        if (uploader == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "*프로필 이미지 업로드 기능이 비활성화 되어 있습니다.");
        }
        S3UploadResult uploadResult = uploader.uploadProfileImage(file, user.getId());
        user.setProfileImageKey(uploadResult.getKey());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return buildUserProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(String loginId, UserUpdateRequest request) {
        User user = userRepository.findByLoginIdAndDeletedFalse(loginId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."));
        ensureNicknameAvailable(request.getNickname(), user);
        ensurePasswordRules(request.getPassword(), request.getPasswordConfirmation());
        user.setNickname(request.getNickname());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        user.setUpdatedAt(LocalDateTime.now());
        return buildUserProfileResponse(user);
    }

    public UserAvailabilityResponse checkLoginId(String loginId) {
        if (userRepository.existsByLoginIdAndDeletedFalse(loginId)) {
            return new UserAvailabilityResponse(false, "*중복된 아이디 입니다.");
        }
        return new UserAvailabilityResponse(true, "사용 가능한 아이디 입니다.");
    }

    public UserAvailabilityResponse checkNickname(String nickname) {
        if (userRepository.existsByNicknameAndDeletedFalse(nickname)) {
            return new UserAvailabilityResponse(false, "*중복된 닉네임 입니다.");
        }
        return new UserAvailabilityResponse(true, "사용 가능한 닉네임 입니다.");
    }

    public UserProfileResponse getProfile(String loginId) {
        User user = userRepository.findByLoginIdAndDeletedFalse(loginId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."));
        log.info("Retrieved profile for loginId={}", loginId);
        return buildUserProfileResponse(user);
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
            resolveProfileImageUrl(summary.getProfileImageKey()),
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
        userRepository.softDelete(user.getId(), LocalDateTime.now());
    }

    private UserProfileResponse buildUserProfileResponse(User user) {
        return new UserProfileResponse(
            user.getId(),
            user.getLoginId(),
            user.getNickname(),
            resolveProfileImageUrl(user.getProfileImageKey())
        );
    }

    private void validateLoginId(String loginId) {
        if (userRepository.existsByLoginIdAndDeletedFalse(loginId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*중복된 아이디 입니다.");
        }
    }

    private void validateNickname(String nickname) {
        if (userRepository.existsByNicknameAndDeletedFalse(nickname)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*중복된 닉네임 입니다.");
        }
    }

    private void ensureNicknameAvailable(String candidate, User currentUser) {
        if (!candidate.equals(currentUser.getNickname()) &&
            userRepository.existsByNicknameAndDeletedFalse(candidate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*중복된 닉네임입니다.");
        }
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
        if (!hasPassword) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*비밀번호를 입력해주세요.");
        }
        if (!hasConfirmation) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*비밀번호를 한 번 더 입력해주세요.");
        }
        if (!password.equals(confirmation)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*비밀번호와 다릅니다.");
        }
        if (password.length() < 8 || password.length() > 20 ||
            !password.matches(".*[A-Z].*") ||
            !password.matches(".*[a-z].*") ||
            !password.matches(".*\\d.*") ||
            password.chars().noneMatch(ch -> !Character.isLetterOrDigit(ch))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "*비밀번호는 8자 이상, 20자 이하이며, 대문자, 소문자, 숫자, 특수문자를 각각 최소 1개 포함해야 합니다.");
        }
    }

    private boolean containsEmoji(String value) {
        return value.codePoints()
            .mapToObj(Character.UnicodeBlock::of)
            .anyMatch(EMOJI_BLOCKS::contains);
    }

    private String resolveProfileImageUrl(String imageKey) {
        return imageUrlResolver.resolve(imageKey);
    }
}
