package com.planit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserSignupRequest {
    @NotBlank(message = "*아이디를 입력해주세요.")
    @Size.List({
        @Size(min = 4, message = "*아이디가 너무 짧습니다"),
        @Size(max = 20, message = "*올바른 아이디 형식을 입력해주세요. 아이디는 4자 이상, 20자 이하이며, 소문자, 숫자, _ 만 포함해야 합니다")
    })
    @Pattern(regexp = "^[a-z0-9_]+$", message = "*올바른 아이디 형식을 입력해주세요. 아이디는 4자 이상, 20자 이하이며, 소문자, 숫자, _ 만 포함해야 합니다")
    private String loginId;

    @NotBlank(message = "*비밀번호를 입력해주세요.")
    @Size(min = 8, max = 20, message = "*비밀번호는 8자 이상, 20자 이하이며, 대문자, 소문자, 숫자, 특수문자를 각각 최소 1개 포함해야 합니다.")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,20}$",
        message = "*비밀번호는 8자 이상, 20자 이하이며, 대문자, 소문자, 숫자, 특수문자를 각각 최소 1개 포함해야 합니다."
    )
    private String password;

    @NotBlank(message = "*닉네임을 입력해주세요")
    @Size(max = 10, message = "*닉네임은 최대 10자까지 작성 가능합니다.")
    @Pattern(regexp = "^[^\\s]+$", message = "*띄어쓰기를 없애주세요")
    private String nickname;

    @NotNull(message = "*프로필 사진을 추가해주세요.")
    private Long profileImageId;
}
