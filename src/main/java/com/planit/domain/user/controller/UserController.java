package com.planit.domain.user.controller; // 사용자 관련 REST 엔드포인트를 모아둔 패키지입니다.

import com.planit.domain.user.dto.SignUpRequest; // 회원가입 요청 payload DTO
import com.planit.domain.user.dto.UserAvailabilityResponse; // 중복 확인 응답 DTO
import com.planit.domain.user.dto.MyPageResponse;
import com.planit.domain.user.dto.UserProfileResponse; // 인증된 사용자 정보 DTO
import com.planit.domain.user.dto.UserSignupResponse; // 회원가입 결과 DTO
import com.planit.domain.user.dto.UserUpdateRequest; // 사용자 정보 수정 요청 DTO
import com.planit.domain.user.service.UserService; // 사용자 도메인 로직을 담당하는 서비스
import jakarta.validation.Valid; // DTO 검증을 위한 표준 애노테이션
import jakarta.validation.constraints.NotBlank; // 빈 문자열 검사
import jakarta.validation.constraints.Pattern; // 정규식 검증
import jakarta.validation.constraints.Size; // 길이 검증
import lombok.RequiredArgsConstructor; // final 필드 자동 생성자 주입
import org.springframework.http.HttpStatus; // HTTP 상태 코드 상수
import org.springframework.validation.annotation.Validated; // 컨트롤러 단위 검증 활성화
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable; // 경로 변수 바인딩
import org.springframework.web.bind.annotation.PostMapping; // POST 매핑
import org.springframework.web.bind.annotation.PutMapping; // PUT 매핑
import org.springframework.web.bind.annotation.RequestBody; // 본문 바인딩
import org.springframework.web.bind.annotation.RequestMapping; // 클래스 단위 경로
import org.springframework.web.bind.annotation.RequestParam; // 쿼리 파라미터 바인딩
import org.springframework.web.bind.annotation.ResponseStatus; // 응답 상태 설정
import org.springframework.web.bind.annotation.RestController; // REST 컨트롤러 선언
import org.springframework.security.core.annotation.AuthenticationPrincipal; // 현재 인증 정보 주입
import org.springframework.security.core.userdetails.UserDetails; // 인증 주체 표현 인터페이스

@RestController // REST 요청 처리를 위한 컨트롤러
@RequestMapping("/users") // 실제 경로는 `/api/users` (context-path `/api` 포함)
@Validated // DTO 유효성 검사를 활성화
@RequiredArgsConstructor // final 필드 생성자 자동 생성
public class UserController {

    private final UserService userService; // 사용자 도메인 서비스를 주입

    @PostMapping("/signup") // POST /api/users/signup
    @ResponseStatus(HttpStatus.CREATED) // 생성 성공 시 201 응답
    public UserSignupResponse signup(@Valid @RequestBody SignUpRequest request) {
        // DTO 검증 후 서비스로 위임하여 저장
        return userService.signup(request);
    }

    @GetMapping("/check-login-id") // GET /api/users/check-login-id?loginId=...
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
        // ID 중복 여부를 응답
        return userService.checkLoginId(loginId);
    }

    @DeleteMapping("/{userId}/profile-image") // DELETE /api/users/{userId}/profile-image
    @ResponseStatus(HttpStatus.NO_CONTENT) // 리소스 없을 때 204 반환
    public void deleteProfileImage(@PathVariable Long userId) {
        userService.deleteProfileImage(userId);
    }

    @GetMapping("/check-nickname") // GET /api/users/check-nickname?nickname=...
    public UserAvailabilityResponse checkNickname(
        @RequestParam
        @NotBlank(message = "*닉네임을 입력해주세요")
        @Size(max = 10, message = "*닉네임은 최대 10자까지 작성 가능합니다.")
        @Pattern(regexp = "^[^\\s]+$", message = "*띄어쓰기를 없애주세요")
        String nickname
    ) {
        return userService.checkNickname(nickname);
    }

    @GetMapping("/me") // GET /api/users/me
    public UserProfileResponse me(@AuthenticationPrincipal UserDetails principal) {
        return userService.getProfile(principal.getUsername());
    }

    @GetMapping("/me/mypage")
    public MyPageResponse myPage(@AuthenticationPrincipal UserDetails principal) {
        return userService.getMyPage(principal.getUsername());
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@AuthenticationPrincipal UserDetails principal) {
        userService.deleteAccount(principal.getUsername());
    }

    @PutMapping("/me") // PUT /api/users/me -> 프로필 수정 요청
    public UserProfileResponse updateProfile(
        @AuthenticationPrincipal UserDetails principal,
        @Valid @RequestBody UserUpdateRequest request
    ) {
        // 인증된 사용자 정보를 기반으로 전달받은 수정 요청을 처리
        return userService.updateProfile(principal.getUsername(), request);
    }
}
