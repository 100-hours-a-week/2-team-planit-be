package com.planit.domain.user.dto; // 사용자 인증 관련 DTO 패키지

import jakarta.validation.constraints.NotBlank; // 빈 문자열 금지
import jakarta.validation.constraints.Pattern; // 정규식 검사
import jakarta.validation.constraints.Size; // 길이 유효성 검사
import lombok.Getter; // Getter 자동 생성
import lombok.NoArgsConstructor; // 기본 생성자 자동화
import lombok.Setter; // Setter 자동 생성

@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "*아이디를 입력해주세요") // 로그인 아이디가 비어있으면 안 됨
    @Size.List({
        @Size(min = 4, message = "*아이디 또는 비밀번호가 잘못 되었습니다. 아이디와 비밀번호를 정확히 입력해 주세요."),
        @Size(max = 20, message = "*아이디 또는 비밀번호가 잘못 되었습니다. 아이디와 비밀번호를 정확히 입력해 주세요.")
    }) // 길이는 4~20자 사이만 허용
    @Pattern(
        regexp = "^[a-z0-9_]+$",
        message = "*아이디 또는 비밀번호가 잘못 되었습니다. 아이디와 비밀번호를 정확히 입력해 주세요."
    ) // 영문 소문자, 숫자, 언더바만 허용
    private String loginId; // 클라이언트가 보내는 로그인 ID

    @NotBlank(message = "*비밀번호를 입력해주세요") // 비밀번호 누락 시 안내 메시지
    private String password; // 평문 비밀번호 (AuthService에서 인코딩 비교)
}
