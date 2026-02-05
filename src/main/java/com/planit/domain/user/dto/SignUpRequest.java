package com.planit.domain.user.dto; // 사용자 인증/회원가입 관련 DTO를 보관하는 패키지

import jakarta.validation.constraints.AssertTrue; // 커스텀 불린 유효성 검사
import jakarta.validation.constraints.NotBlank; // 빈 문자열 여부 확인
import jakarta.validation.constraints.Pattern; // 정규식 기반 검증
import jakarta.validation.constraints.Size; // 길이 제한 검증
import lombok.Getter; // Getter 자동 생성
import lombok.NoArgsConstructor; // 기본 생성자 자동 생성
import lombok.Setter; // Setter 자동 생성

@Getter
@Setter
@NoArgsConstructor
public class SignUpRequest {
    @NotBlank(message = "*아이디를 입력해주세요.") // 비어있는 아이디는 허용 안 함
    @Size.List({
        @Size(min = 4, message = "*아이디가 너무 짧습니다"),
        @Size(max = 20, message = "*올바른 아이디 형식을 입력해주세요. 아이디는 4자 이상, 20자 이하이며, 영문 소문자와 숫자, _ 만 포함해야 합니다")
    }) // 길이 4~20자 제한
    @Pattern(
        regexp = "^[a-z0-9_]+$",
        message = "*아이디는 영문 소문자와 숫자, _ 만 포함할 수 있습니다."
    ) // 영문 소문자/숫자/언더바만 허용
    private String loginId; // 사용자 선택 로그인 아이디

    @NotBlank(message = "*비밀번호를 입력해주세요.") // 비밀번호 유무 체크
    @Size(
        min = 8,
        max = 20,
        message = "*비밀번호는 8자 이상, 20자 이하이며, 대문자, 소문자, 숫자, 특수문자를 각각 최소 1개 포함해야 합니다."
    ) // 길이 8~20자 제한
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,20}$",
        message = "*비밀번호는 8자 이상, 20자 이하이며, 대문자, 소문자, 숫자, 특수문자를 각각 최소 1개 포함해야 합니다."
    ) // 복합 패턴 검증
    private String password; // 평문 비밀번호

    @NotBlank(message = "*비밀번호를 입력해주세요.") // 확인용 비밀번호 누락 체크
    private String passwordConfirm; // 비밀번호 확인 입력값

    @NotBlank(message = "*닉네임을 입력해주세요")
    @Size(max = 10, message = "*닉네임은 최대 10자까지 작성 가능합니다.")
    @Pattern(regexp = "^[^\\s]+$", message = "*띄어쓰기를 없애주세요")
    private String nickname; // 사용자 닉네임

    /** Presigned URL로 S3 업로드 완료 후 전달하는 프로필 이미지 key (선택). signup/ prefix 사용 */
    @Size(max = 500, message = "*프로필 이미지 key가 올바르지 않습니다.")
    private String profileImageKey;

    @AssertTrue(message = "*비밀번호가 다릅니다.")
    private boolean isPasswordMatching() {
        if (password == null || passwordConfirm == null) {
            return true; // 둘 다 null이면 다른 검증에서 잡음
        }
        return password.equals(passwordConfirm); // 비밀번호 일치 여부
    }
}
