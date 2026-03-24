package com.planit.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.planit.domain.post.repository.PostRepository;
import com.planit.domain.user.dto.UserCreateRequest;
import com.planit.domain.user.dto.UserProfileResponse;
import com.planit.domain.user.dto.UserUpdateRequest;
import com.planit.domain.user.entity.User;
import com.planit.domain.user.repository.UserRepository;
import com.planit.domain.user.service.UserService;
import com.planit.domain.user.service.support.UserConstraintMetadata;
import com.planit.global.common.exception.BusinessException;
import com.planit.global.common.exception.ErrorCode;
import com.planit.infrastructure.storage.S3ImageUrlResolver;
import com.planit.infrastructure.storage.UploadUrlProvider;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ObjectProvider<UploadUrlProvider> uploadUrlProvider;
    @Mock
    private S3ImageUrlResolver imageUrlResolver;
    @Mock
    private UserConstraintMetadata userConstraintMetadata;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("회원_생성_성공")
    void 회원_생성_성공() {
        UserCreateRequest request = new UserCreateRequest();
        request.setNickname("planit");
        request.setEmail("test@planit.com");
        request.setPassword("password123");

        when(userRepository.existsByLoginIdAndDeletedFalse("test@planit.com")).thenReturn(false);
        when(userRepository.existsByNicknameAndDeletedFalse("planit")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        UserProfileResponse response = userService.createUser(request);

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getLoginId()).isEqualTo("test@planit.com");
        assertThat(response.getNickname()).isEqualTo("planit");
    }

    @Test
    @DisplayName("이메일_중복이면_예외_발생")
    void 이메일_중복이면_예외_발생() {
        UserCreateRequest request = new UserCreateRequest();
        request.setNickname("planit");
        request.setEmail("dup@planit.com");
        request.setPassword("password123");
        when(userRepository.existsByLoginIdAndDeletedFalse("dup@planit.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    @DisplayName("닉네임_중복이면_예외_발생")
    void 닉네임_중복이면_예외_발생() {
        UserCreateRequest request = new UserCreateRequest();
        request.setNickname("dupNick");
        request.setEmail("user@planit.com");
        request.setPassword("password123");
        when(userRepository.existsByLoginIdAndDeletedFalse("user@planit.com")).thenReturn(false);
        when(userRepository.existsByNicknameAndDeletedFalse("dupNick")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_NICKNAME);
    }

    @Test
    @DisplayName("존재하지_않는_유저_조회시_USER_NOT_FOUND")
    void 존재하지_않는_유저_조회시_USER_NOT_FOUND() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(99L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("회원_수정_성공")
    void 회원_수정_성공() {
        User user = activeUser(1L, "before@planit.com", "beforeNick");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByLoginIdAndDeletedFalse("after@planit.com")).thenReturn(false);
        when(userRepository.existsByNicknameAndDeletedFalse("afterNick")).thenReturn(false);
        when(passwordEncoder.encode("newpassword")).thenReturn("encoded-new");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserUpdateRequest request = new UserUpdateRequest();
        request.setEmail("after@planit.com");
        request.setNickname("afterNick");
        request.setPassword("newpassword");

        UserProfileResponse response = userService.updateUser(1L, request);

        assertThat(response.getLoginId()).isEqualTo("after@planit.com");
        assertThat(response.getNickname()).isEqualTo("afterNick");
    }

    @Test
    @DisplayName("회원_삭제_성공")
    void 회원_삭제_성공() {
        User user = activeUser(1L, "user@planit.com", "nick");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.deleteUser(1L);

        verify(userRepository).save(user);
        assertThat(user.isDeleted()).isTrue();
    }

    private User activeUser(Long id, String loginId, String nickname) {
        User user = new User();
        user.setId(id);
        user.setLoginId(loginId);
        user.setPassword("encoded");
        user.setNickname(nickname);
        user.setDeleted(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}
