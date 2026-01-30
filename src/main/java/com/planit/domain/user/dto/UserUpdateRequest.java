package com.planit.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UserUpdateRequest {
    @NotBlank(message = "*닉네임을 입력해주세요.")
    @Size(max = 10, message = "*닉네임은 최대 10자까지 작성 가능합니다.")
    @Pattern(regexp = "^[^\\s]+$", message = "*닉네임에는 공백을 사용할 수 없습니다.")
    private String nickname; // 닉네임 helper text/유효성 기준 (10자 이내, 공백/이모지 금지)

    @Size(min = 8, max = 20, message = "*비밀번호는 8자 이상, 20자 이하이며, 대문자, 소문자, 숫자, 특수문자를 각각 최소 1개 포함해야 합니다.")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*\\W).*$",
        message = "*비밀번호는 8자 이상, 20자 이하이며, 대문자, 소문자, 숫자, 특수문자를 각각 최소 1개 포함해야 합니다."
    )
    private String password; // 비밀번호 새 입력 (포커스 아웃/저장 시 유효성 체크)

    private String passwordConfirm; // 비밀번호 확인 (입력 없을 경우 요청 거부)

    private Long profileImageId; // 프로필 이미지 변경 시 이미지 ID 전달
}
