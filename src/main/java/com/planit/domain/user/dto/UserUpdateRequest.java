package com.planit.domain.user.dto; // 사용자 정보 수정 payload DTO 모아두는 패키지입니다.

import jakarta.validation.constraints.NotBlank; // 필수 입력 확인용 애노테이션
import jakarta.validation.constraints.Pattern; // 정규식 검증
import jakarta.validation.constraints.Size; // 길이 제한 검증
import lombok.AllArgsConstructor; // 전체 필드 생성자 자동 생성
import lombok.Getter; // 게터 자동 생성
import lombok.NoArgsConstructor; // 파라미터 없는 생성자 자동 생성
import lombok.Setter; // 세터 자동 생성

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    @NotBlank(message = "*닉네임을 입력해주세요.") // 닉네임 미입력 시 메시지
    @Size(max = 10, message = "*닉네임은 최대 10자까지 작성 가능합니다.") // 최대 길이 제한
    @Pattern(regexp = "^[^\\s]+$", message = "*닉네임에는 공백을 사용할 수 없습니다.") // 공백 금지
    private String nickname; // 프론트에서 보여줄 닉네임

    @Size(min = 8, max = 20, message = "*비밀번호는 8자 이상, 20자 이하이며, 대문자, 소문자, 숫자, 특수문자를 각각 최소 1개 포함해야 합니다.")
    private String password; // 변경할 비밀번호 (선택 입력)

    private String passwordConfirmation; // 비밀번호 확인 필드 (입력 시 password와 일치해야 함)

    private Long profileImageId; // 프로필 이미지 변경 시 전달되는 이미지 ID
}
