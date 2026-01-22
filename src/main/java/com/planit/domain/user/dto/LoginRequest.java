package com.planit.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "*아이디를 입력해주세요")
    @Size.List({
        @Size(min = 4, message = "*아이디 또는 비밀번호가 잘못 되었습니다. 아이디와 비밀번호를 정확히 입력해 주세요."),
        @Size(max = 20, message = "*아이디 또는 비밀번호가 잘못 되었습니다. 아이디와 비밀번호를 정확히 입력해 주세요.")
    })
    @Pattern(
        regexp = "^[a-z0-9_]+$",
        message = "*아이디 또는 비밀번호가 잘못 되었습니다. 아이디와 비밀번호를 정확히 입력해 주세요."
    )
    private String loginId;

    @NotBlank(message = "*비밀번호를 입력해주세요")
    private String password;
}
