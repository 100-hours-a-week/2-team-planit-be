package com.planit.service;

import com.planit.domain.User;
import com.planit.domain.UserProfileImage;
import com.planit.dto.SignUpRequest;
import com.planit.dto.UserAvailabilityResponse;
import com.planit.dto.UserSignupResponse;
import com.planit.repository.UserProfileImageRepository;
import com.planit.repository.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserProfileImageRepository userProfileImageRepository;
    private final PasswordEncoder passwordEncoder;

    public UserSignupResponse signup(SignUpRequest request) {
        if (userRepository.existsByLoginIdAndDeletedFalse(request.getLoginId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*중복된 아이디 입니다.");
        }
        if (userRepository.existsByNicknameAndDeletedFalse(request.getNickname())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*중복된 닉네임 입니다.");
        }

        User user = new User();
        user.setLoginId(request.getLoginId());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setDeleted(false);
        User saved = userRepository.save(user);

        UserProfileImage profileImage = new UserProfileImage();
        profileImage.setUserId(saved.getId());
        profileImage.setImageId(request.getProfileImageId());
        userProfileImageRepository.save(profileImage);

        return new UserSignupResponse(saved.getId());
    }

    @Transactional
    public void deleteProfileImage(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."));
        if (user.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다.");
        }
        if (!userProfileImageRepository.existsByUserId(userId)) {
            return;
        }
        userProfileImageRepository.deleteByUserId(userId);
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
}
