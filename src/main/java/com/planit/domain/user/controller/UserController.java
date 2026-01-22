package com.planit.domain.user.controller;

import com.planit.domain.user.dto.SignUpRequest;
import com.planit.domain.user.dto.UserAvailabilityResponse;
import com.planit.domain.user.dto.UserSignupResponse;
import com.planit.domain.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@Validated
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserSignupResponse signup(@Valid @RequestBody SignUpRequest request) {
        return userService.signup(request);
    }

    @GetMapping("/check-login-id")
    public UserAvailabilityResponse checkLoginId(
        @RequestParam
        @NotBlank(message = "*아이디를 입력해주세요.")
        @Size.List({
            @Size(min = 4, message = "*아이디가 너무 짧습니다"),
            @Size(max = 20, message = "*아이디는 최대 20자까지 작성 가능합니다.")
        })
        @Pattern(
            regexp = "^[a-z0-9_]+$",
            message = "*아이디는 영문 소문자와 숫자, _ 만 포함할 수 있습니다."
        )
        String loginId
    ) {
        return userService.checkLoginId(loginId);
    }

    @DeleteMapping("/{userId}/profile-image")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProfileImage(@PathVariable Long userId) {
        userService.deleteProfileImage(userId);
    }

    @GetMapping("/check-nickname")
    public UserAvailabilityResponse checkNickname(
        @RequestParam
        @NotBlank(message = "*닉네임을 입력해주세요")
        @Size(max = 10, message = "*닉네임은 최대 10자까지 작성 가능합니다.")
        @Pattern(regexp = "^[^\\s]+$", message = "*띄어쓰기를 없애주세요")
        String nickname
    ) {
        return userService.checkNickname(nickname);
    }
}
