package com.planit.domain.user.controller; // 사용자 관련 REST 엔드포인트를 모아둔 패키지입니다.

import com.planit.domain.user.dto.SignUpRequest; // 회원가입 요청 payload DTO
import com.planit.domain.user.dto.UserAvailabilityResponse; // 중복 확인 응답 DTO
import com.planit.domain.user.dto.UserProfileResponse; // 인증된 사용자 정보 DTO
import com.planit.domain.user.dto.UserSignupResponse; // 회원가입 결과 DTO
import com.planit.domain.user.dto.UserUpdateRequest; // 회원 정보 수정 요청 DTO
import com.planit.domain.user.service.UserService; // 사용자 도메인 로직을 담당하는 서비스
import jakarta.validation.Valid; // DTO 검증을 위한 표준 애노테이션
import jakarta.validation.constraints.NotBlank; // 빈 문자열 검사
import jakarta.validation.constraints.Pattern; // 정규식 검증
import jakarta.validation.constraints.Size; // 길이 검증
import lombok.RequiredArgsConstructor; // final 필드 자동 생성자 주입
import org.springframework.http.HttpStatus; // HTTP 상태 코드 상수
import org.springframework.validation.annotation.Validated; // 컨트롤러 단위 검증 활성화
import org.springframework.web.bind.annotation.DeleteMapping; // DELETE 매핑
import org.springframework.web.bind.annotation.GetMapping; // GET 매핑
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

@RestController // JSON REST 컨트롤러로 마이페이지 연동 전용 API 노출
@RequestMapping("/users") // `/api/users` 기반 경로 선행 처리
@Validated // DTO 레벨 유효성 검사를 클래스 단계에서 적용
@RequiredArgsConstructor // 서비스 자동 주입 생성자 제공
public class UserController {

    private final UserService userService; // 사용자 도메인 서비스를 주입

    @PostMapping("/signup") // POST /api/users/signup
    @ResponseStatus(HttpStatus.CREATED) // 생성 성공 시 201 응답
    public UserSignupResponse signup(@Valid @RequestBody SignUpRequest request) {
        // DTO를 통해 받은 가입 필드로 회원가입 처리
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
        // helper text 기준으로 중복된 ID 여부를 단순 메시지로 응답
        return userService.checkLoginId(loginId);
    }

    @DeleteMapping("/{userId}/profile-image") // DELETE /api/users/{userId}/profile-image
    @ResponseStatus(HttpStatus.NO_CONTENT) // 프로필 이미지 삭제 후 204 반환
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
        // 마이페이지 진입 시 필요한 최소 프로필 정보를 조회
        return userService.getProfile(principal.getUsername());
    }

    @PutMapping("/me") // 회원 정보 수정 엔드포인트
    public UserProfileResponse updateProfile(
        @AuthenticationPrincipal UserDetails principal,
        @Valid @RequestBody UserUpdateRequest request
    ) {
        // 닉네임/비밀번호/이미지 유효성 통과 시 service에서 저장 후 최신 프로필 반환
        return userService.updateProfile(principal.getUsername(), request);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@AuthenticationPrincipal UserDetails principal) {
        // 팀 정책에 따라 soft-delete 처리 및 이미지 제거
        userService.deleteAccount(principal.getUsername());
    }

}
